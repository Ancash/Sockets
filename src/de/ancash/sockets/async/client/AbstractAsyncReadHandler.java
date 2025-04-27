package de.ancash.sockets.async.client;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.ancash.sockets.async.ByteEventHandler;
import de.ancash.sockets.io.ByteBufferDistributor;
import de.ancash.sockets.io.DistributedByteBuffer;

public abstract class AbstractAsyncReadHandler implements CompletionHandler<Integer, DistributedByteBuffer> {

	static final ExecutorService exec = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 8, 1),
			new ThreadFactory() {
				AtomicInteger cnt = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "ReadHandler-" + cnt.getAndIncrement());
				}
			});

	protected final AbstractAsyncClient client;
	protected ByteEventHandler byteHandler;
	AtomicBoolean reading = new AtomicBoolean(false);
	AtomicBoolean completing = new AtomicBoolean(false);
	static final int bufCnt = 4;

	public AbstractAsyncReadHandler(AbstractAsyncClient asyncClient, int readBufSize, ByteEventHandler byteHandler) {
		this.client = asyncClient;
		this.byteHandler = byteHandler;
		bbd = new ByteBufferDistributor(readBufSize, bufCnt);
	}

	LinkedBlockingQueue<DistributedByteBuffer> toDo = new LinkedBlockingQueue<DistributedByteBuffer>();
	ByteBufferDistributor bbd;

	long lastRead;

	@Override
	public void completed(Integer read, DistributedByteBuffer buf) {
		if (read == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), buf);
			return;
		}
		lastRead = System.nanoTime();
		buf.buffer.flip();
		toDo.add(buf);
		if (!completing.get()) {
			long d = lastRead;
			exec.submit(() -> complete(d, 0));
		}
		initRead();
	}

	void complete(long l, int cnt) {
		if (lastRead != l && bbd.isBufferAvailable() && bufCnt / 2 < cnt) {
			long d = lastRead;
			exec.submit(() -> complete(d, cnt + 1));
			return;
		}
		if (!completing.compareAndSet(false, true)) {
			return;
		}
		while (!toDo.isEmpty()) {
			DistributedByteBuffer next = toDo.poll();
			if (client.readHandler.byteHandler != null)
				client.readHandler.byteHandler.onBytes(next.buffer);
			else
				client.onBytesReceive(next.buffer);
			bbd.freeBuffer(next);
		}
		completing.set(false);
	}

	public boolean tryInitRead() {
		if (!client.isConnected() || !reading.compareAndSet(false, true)) {
			return false;
		}
		initRead();
		return true;
	}

	private void initRead() {
		DistributedByteBuffer readBuf;
		readBuf = bbd.getBufferBlocking();
		client.getAsyncSocketChannel().read(readBuf.buffer, readBuf, this);
	}

	@Override
	public void failed(Throwable arg0, DistributedByteBuffer arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}

	public abstract void onDisconnect();
}