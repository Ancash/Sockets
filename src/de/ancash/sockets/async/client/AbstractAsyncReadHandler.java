package de.ancash.sockets.async.client;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import de.ancash.Sockets;
import de.ancash.datastructures.tuples.Duplet;
import de.ancash.sockets.async.ByteEventHandler;
import de.ancash.sockets.io.ByteBufferDistributor;
import de.ancash.sockets.io.PositionedByteBuf;

public abstract class AbstractAsyncReadHandler implements CompletionHandler<Integer, ByteBuffer> {

	static final ConcurrentHashMap<Integer, AbstractAsyncClient> clients = new ConcurrentHashMap<>();
	static final ConcurrentHashMap<Integer, LinkedBlockingQueue<PositionedByteBuf>> bufs = new ConcurrentHashMap<>();
	static final ConcurrentHashMap<Integer, AtomicBoolean> blocked = new ConcurrentHashMap<>();
	static final LinkedBlockingQueue<Duplet<AbstractAsyncClient, PositionedByteBuf>> toProcess = new LinkedBlockingQueue<>();
	static final ExecutorService byteHandlerPool = Executors
			.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 2 + 1, 1), new ThreadFactory() {
				int i = 0;

				@SuppressWarnings("nls")
				@Override
				public synchronized Thread newThread(Runnable r) {
					Thread t = new Thread(r, "AsyncReadBytesHandler-" + i++);
					t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

						@Override
						public void uncaughtException(Thread arg0, Throwable arg1) {
							System.err.println(arg0.getName() + " threw exception while reading");
							arg1.printStackTrace();
						}
					});
					return t;
				}
			});

	static {
		for (int i = 0; i < Math.max(Runtime.getRuntime().availableProcessors() / 2 + 1, 1); i++) {
			byteHandlerPool.submit(() -> {
				while (!Thread.interrupted()) {
					boolean b = false;
					for (int k : bufs.keySet()) {
						if (bufs.get(k).isEmpty() || blocked.get(k).get())
							continue;
						if (!blocked.get(k).compareAndSet(false, true))
							continue;
						LinkedBlockingQueue<PositionedByteBuf> queue = bufs.get(k);
						AbstractAsyncClient client = clients.get(k);
						while (!queue.isEmpty()) {
							PositionedByteBuf pbb = queue.poll();
							pbb.owner = Thread.currentThread();
							if (client.readHandler.byteHandler != null)
								client.readHandler.byteHandler.onBytes(pbb.get());
							else
								client.onBytesReceive(pbb.get());

						}
						b = true;
						blocked.get(k).compareAndSet(true, false);
					}
					if (!b) {
						Sockets.sleep(10_000);
					}
				}
			});
		}
	}

	static void queueBuf(AbstractAsyncClient client, PositionedByteBuf pbb) throws InterruptedException {
		clients.computeIfAbsent(client.instance, k -> client);
		blocked.computeIfAbsent(client.instance, k -> new AtomicBoolean(false));
		bufs.computeIfAbsent(client.instance, k -> new LinkedBlockingQueue<>(1000)).put(pbb);
	}

	protected final AbstractAsyncClient client;
	protected ByteEventHandler byteHandler;
	long lastRead = System.nanoTime();
//	protected final ByteBufferDistributor fbb;

	public AbstractAsyncReadHandler(AbstractAsyncClient asyncClient, int readBufSize, ByteEventHandler byteHandler) {
		this.client = asyncClient;
		this.byteHandler = byteHandler;
//		fbb = new ByteBufferDistributor(readBufSize, 64);
	}

	@Override
	public void completed(Integer read, ByteBuffer buf) {
		if (read == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), buf);
			return;
		}
		client.reading.set(false);
		buf.flip();
		try {
			queueBuf(client, new PositionedByteBuf(buf, -1));
		} catch (InterruptedException e) {
			failed(e, buf);
			return;
		}
		client.reading.set(true);
		buf = ByteBuffer.allocate(client.readBufSize);
		client.getAsyncSocketChannel().read(buf, client.timeout, client.timeoutunit, buf, this);
	}

	@Override
	public void failed(Throwable arg0, ByteBuffer arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}

	public abstract void onDisconnect();
}