package de.ancash.sockets.async.client;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.WritePendingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.ancash.Sockets;
import de.ancash.sockets.io.PositionedByteBuf;

public abstract class AbstractAsyncWriteHandler implements CompletionHandler<Integer, PositionedByteBuf> {

	static final ExecutorService writerPool = Executors.newFixedThreadPool(1, new ThreadFactory() {

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
	public Thread writer;

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
			writerPool.submit(() -> {
				writer = Thread.currentThread();
				client.getAsyncSocketChannel().write(bb.get(), bb, this);
			});
		} else {
			client.fbb.unblock(bb);
			lastWrite = System.nanoTime();
			writer = null;
			client.writing.set(false);
			writerPool.submit(() -> {
				canWrite.set(true);
				try {
					client.checkWrite();
				} catch (InterruptedException e) {
					failed(e, bb);
				}
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
		client.writing.set(true);
		writerPool.submit(() -> {
			writer = Thread.currentThread();
			client.getAsyncSocketChannel().write(bb.get(), bb, this);
		});
		return true;
	}
}