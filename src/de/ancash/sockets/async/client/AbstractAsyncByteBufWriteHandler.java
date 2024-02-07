package de.ancash.sockets.async.client;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.WritePendingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import de.ancash.sockets.io.PositionedByteBuf;

public abstract class AbstractAsyncByteBufWriteHandler implements CompletionHandler<Integer, PositionedByteBuf> {

	static final ExecutorService writerPool = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 6),
			new ThreadFactory() {

				int cnt = 0;

				@Override
				public synchronized Thread newThread(Runnable r) {
					Thread t = new Thread(r, "AsyncWriteHandler-" + cnt++);
					t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

						@Override
						public void uncaughtException(Thread arg0, Throwable arg1) {
							System.err.println(arg0.getName() + " threw exception while writing");
							arg1.printStackTrace();
						}
					});
					return t;
				}
			});

	public static class CheckWriteEvent {
		public AbstractAsyncClient client;
		Runnable r;
	}

	protected final AbstractAsyncClient client;
	protected AtomicBoolean canWrite = new AtomicBoolean(true);
	protected long lastWrite;

	public AbstractAsyncByteBufWriteHandler(AbstractAsyncClient asyncSocket) {
		this.client = asyncSocket;
	}

	@Override
	public void completed(Integer written, PositionedByteBuf bb) {
		if (written == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), bb);
			return;
		}

		if (bb.get().hasRemaining()) {
			client.getAsyncSocketChannel().write(bb.get(), bb, this);
//			writerPool.submit(() -> client.getAsyncSocketChannel().write(bb.get(), bb, this));
		} else {
			client.fbb.unblock(bb);
			lastWrite = System.nanoTime();
			client.writing.set(false);
			writerPool.submit(() -> {
				canWrite.set(true);
				client.checkWrite();
			});
		}
	}

	@Override
	public void failed(Throwable arg0, PositionedByteBuf arg1) {
		arg0.printStackTrace();
		client.setConnected(false);
		client.onDisconnect(arg0);
	}

	public boolean canWrite() {
		return canWrite.get();
	}

	public boolean write(PositionedByteBuf bb) {
		if (!canWrite.compareAndSet(true, false))
			throw new WritePendingException();
		client.writing.set(true);
		writerPool.submit(() -> client.getAsyncSocketChannel().write(bb.get(), bb, this));
		return true;
	}
}