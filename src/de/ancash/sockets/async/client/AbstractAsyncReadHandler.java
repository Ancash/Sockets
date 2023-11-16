package de.ancash.sockets.async.client;

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
import de.ancash.sockets.async.ByteEventHandler;
import de.ancash.sockets.io.ByteBufferDistributor;
import de.ancash.sockets.io.PositionedByteBuf;

public abstract class AbstractAsyncReadHandler implements CompletionHandler<Integer, PositionedByteBuf> {

	static final ConcurrentHashMap<Integer, AbstractAsyncClient> clients = new ConcurrentHashMap<>();
	static final ConcurrentHashMap<Integer, LinkedBlockingQueue<PositionedByteBuf>> bufs = new ConcurrentHashMap<>();
	static final ConcurrentHashMap<Integer, AtomicBoolean> blocked = new ConcurrentHashMap<>();
	static final ExecutorService byteHandlerPool = Executors
			.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1), new ThreadFactory() {
				int i = 0;

				@SuppressWarnings("nls")
				@Override
				public synchronized Thread newThread(Runnable r) {
					return new Thread(r, "AsyncReadBytesHandler-" + i++);
				}
			});

	static {
		for (int i = 0; i < Math.max(Runtime.getRuntime().availableProcessors() / 2, 1); i++) {
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
							System.out.println(Thread.currentThread().getName() + " blocking " + pbb.getAId() + " " + pbb.getBId());
							try {
								pbb.owner = Thread.currentThread();
								ByteBuffer bb = ByteBuffer.allocate(pbb.get().remaining());
								bb.put(pbb.get());
								bb.position(0);
								if (client.readHandler.byteHandler != null)
									client.readHandler.byteHandler.onBytes(bb);
								else
									client.onBytesReceive(bb);

							} finally {
								System.out.println(Thread.currentThread().getName() + " unblocked " + pbb.getAId() + " " + pbb.getBId());
								client.readHandler.fbb.unblockBuffer(pbb);
							}
						}
						b = true;
						blocked.get(k).compareAndSet(true, false);
					}
					if (!b)
						Sockets.sleep(10_000);
				}
			});
		}
	}

	static void queueBuf(AbstractAsyncClient client, PositionedByteBuf pbb) {
		clients.computeIfAbsent(client.instance, k -> client);
		blocked.computeIfAbsent(client.instance, k -> new AtomicBoolean(false));
		bufs.computeIfAbsent(client.instance, k -> new LinkedBlockingQueue<>(client.readHandler.fbb.capacity()))
				.add(pbb);
	}

	protected final AbstractAsyncClient client;
	protected ByteEventHandler byteHandler;
	long lastRead = System.nanoTime();
	protected final ByteBufferDistributor fbb;

	public AbstractAsyncReadHandler(AbstractAsyncClient asyncClient, int readBufSize, ByteEventHandler byteHandler) {
		this.client = asyncClient;
		this.byteHandler = byteHandler;
		fbb = new ByteBufferDistributor(readBufSize, 32);
	}

	@Override
	public void completed(Integer read, PositionedByteBuf buf) {
		if (read == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), buf);
			return;
		}
		buf.get().flip();
		queueBuf(client, buf);
		while (!fbb.isBufferAvailable()) {
			Sockets.sleep(10_000);
		}
		PositionedByteBuf next = fbb.getAvailableBuffer();
		client.getAsyncSocketChannel().read(next.get(), client.timeout, client.timeoutunit, next, this);
	}

	@Override
	public void failed(Throwable arg0, PositionedByteBuf arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}

	public abstract void onDisconnect();
}