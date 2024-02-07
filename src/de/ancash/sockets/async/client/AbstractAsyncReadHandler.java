package de.ancash.sockets.async.client;

import java.lang.Thread.UncaughtExceptionHandler;
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
	static final ConcurrentHashMap<Integer, AtomicBoolean> blocked = new ConcurrentHashMap<Integer, AtomicBoolean>();
	static final LinkedBlockingQueue<Integer> toProcess = new LinkedBlockingQueue<>();
	static final ExecutorService byteHandlerPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
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

//	static final Object wait = new Object();

	static {
		for (int i = 0; i < 2; i++) {
			byteHandlerPool.submit(() -> {
				try {
					while (!Thread.interrupted()) {
						int next = toProcess.take();
						if (bufs.get(next).isEmpty() || blocked.computeIfAbsent(next, f -> new AtomicBoolean(false)).get())
							continue;
						if (!blocked.get(next).compareAndSet(false, true))
							continue;

						LinkedBlockingQueue<PositionedByteBuf> queue = bufs.get(next);
						AbstractAsyncClient client = clients.get(next);
						int cnt = 0;
						while (!queue.isEmpty() && cnt++ < 100) {
							PositionedByteBuf pbb = queue.poll();
							pbb.owner = Thread.currentThread();
							if (client.readHandler.byteHandler != null)
								client.readHandler.byteHandler.onBytes(pbb.get());
							else
								client.onBytesReceive(pbb.get());
							client.readHandler.fbb.unblockBuffer(pbb);
						}
						if (!queue.isEmpty())
							toProcess.add(next);
						blocked.get(next).set(false);
					}
				} catch (Throwable th) {
					th.printStackTrace();
				}
			});
		}
	}

	static void queueBuf(AbstractAsyncClient client, PositionedByteBuf pbb) throws InterruptedException {
		clients.computeIfAbsent(client.instance, k -> client);
		bufs.computeIfAbsent(client.instance, k -> new LinkedBlockingQueue<>(1000)).put(pbb);
		if (!toProcess.contains(client.instance))
			toProcess.add(client.instance);
	}

	protected final AbstractAsyncClient client;
	protected ByteEventHandler byteHandler;
	long lastRead = System.nanoTime();
	protected final ByteBufferDistributor fbb;

	public AbstractAsyncReadHandler(AbstractAsyncClient asyncClient, int readBufSize, ByteEventHandler byteHandler) {
		this.client = asyncClient;
		this.byteHandler = byteHandler;
		fbb = new ByteBufferDistributor(readBufSize, 64);
	}

	@Override
	public void completed(Integer read, PositionedByteBuf buf) {
		if (read == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), buf);
			return;
		}
		client.reading.set(false);
		buf.get().flip();
		try {
			queueBuf(client, buf);
		} catch (InterruptedException e) {
			failed(e, buf);
			return;
		}
		if (client.delayNextRead()) {
			new Thread(() -> {
				Sockets.sleepMillis(1);
				initRead();
			}).start();
			return;
		} else
			initRead();
	}

	private void initRead() {
		while (!fbb.isBufferAvailable()) {
			Sockets.sleepMillis(1);
		}
		PositionedByteBuf buf = fbb.getAvailableBuffer();
		client.reading.set(true);
		client.getAsyncSocketChannel().read(buf.get(), client.timeout, client.timeoutunit, buf, this);
	}

	@Override
	public void failed(Throwable arg0, PositionedByteBuf arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}

	public abstract void onDisconnect();
}