package de.ancash.sockets.async.client;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.WritePendingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import de.ancash.sockets.io.PositionedByteBuf;

public abstract class AbstractAsyncWriteHandler implements CompletionHandler<Integer, PositionedByteBuf> {

	static final ExecutorService writerPool = Executors
			.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1), new ThreadFactory() {

				int cnt = 0;

				@Override
				public synchronized Thread newThread(Runnable r) {
					return new Thread(r, "AsyncWriteHandler-" + cnt++);
				}
			});

	public static class CheckWriteEvent {
		public AbstractAsyncClient client;
		Runnable r;
	}

	protected final AbstractAsyncClient client;
	protected AtomicBoolean canWrite = new AtomicBoolean(true);
	protected long lastWrite;

	public AbstractAsyncWriteHandler(AbstractAsyncClient asyncSocket) {
		this.client = asyncSocket;
	}

	@Override
	public void completed(Integer written, PositionedByteBuf bb) {
		if (written == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), bb);
			return;
		}
		if (bb.get().hasRemaining()) {
			writerPool.submit(() -> client.getAsyncSocketChannel().write(bb.get(), bb, this));
		} else {
			client.fbb.unblock(bb);
			lastWrite = System.nanoTime();
			writerPool.submit(() -> {
				client.writeHandler.canWrite.set(true);
				client.checkWrite();
			});
		}

	}

	@Override
	public void failed(Throwable arg0, PositionedByteBuf arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}

	public boolean canWrite() {
		return canWrite.get();
	}

	public boolean write(PositionedByteBuf bb) {
		if (!canWrite.compareAndSet(true, false))
			throw new WritePendingException();
		writerPool.submit(() -> client.getAsyncSocketChannel().write(bb.get(), bb, this));
		return true;
	}
}