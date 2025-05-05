package de.ancash.sockets.async.client;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.ancash.sockets.io.DistributedByteBuffer;
import de.ancash.sockets.io.RotatingBuffer;

public class DefaultAsyncWriteHandler implements IWriteHandler, CompletionHandler<Long, DistributedByteBuffer[]> {

	static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 4, 1),
			new ThreadFactory() {
				AtomicInteger cnt = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "ToiletFlusher-" + cnt.getAndIncrement());
				}
			});

	protected final AbstractAsyncClient client;
	protected AtomicBoolean canWrite = new AtomicBoolean(true);
	protected final RotatingBuffer writeBuf;

	public DefaultAsyncWriteHandler(AbstractAsyncClient asyncSocket) {
		this.client = asyncSocket;
		writeBuf = new RotatingBuffer(asyncSocket.writeBufSize, 16);
	}

	@Override
	public void completed(Long written, DistributedByteBuffer[] bufs) {
		if (written == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), bufs);
			return;
		}
		List<DistributedByteBuffer> toWrite = new ArrayList<DistributedByteBuffer>();
		for (DistributedByteBuffer buf : bufs) {
			if (buf.buffer.hasRemaining()) {
				toWrite.add(buf);
			} else {
				writeBuf.free(buf);
			}
		}
		if (!toWrite.isEmpty()) {
			client.getAsyncSocketChannel().write(toWrite.stream().map(d -> d.buffer).toArray(ByteBuffer[]::new), 0, toWrite.size(), 10,
					TimeUnit.SECONDS, toWrite.toArray(DistributedByteBuffer[]::new), this);
		} else {
			canWrite.set(true);
			checkWrite();
		}
	}

	@Override
	public void failed(Throwable arg0, DistributedByteBuffer[] arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
		arg0.printStackTrace();
	}

	long lastWriteInit;

	public void checkWrite() {
		if (canWrite.compareAndSet(true, false)) {
			List<DistributedByteBuffer> toWrite = new ArrayList<DistributedByteBuffer>();
			DistributedByteBuffer write = null;
			while ((write = writeBuf.read()) != null) {
				toWrite.add(write);
			}
			if (toWrite.isEmpty()) {
				canWrite.set(true);
				return;
			}
			lastWriteInit = System.nanoTime();
			client.getAsyncSocketChannel().write(toWrite.stream().map(d -> d.buffer).toArray(ByteBuffer[]::new), 0, toWrite.size(), 10,
					TimeUnit.SECONDS, toWrite.toArray(DistributedByteBuffer[]::new), this);

		}

	}

	Lock lock = new ReentrantLock();

	public void write(ByteBuffer bb) {
		if (!client.isConnected()) {
			return;
		}
		try {
			lock.lock();
			while (bb.hasRemaining()) {
				try {
					if (writeBuf.write(bb) > 0) {
						if (!bb.hasRemaining()) {
							break;
						}
					} else {
						checkWrite();
						writeBuf.writeBlocking(bb);
					}
				} catch (InterruptedException e) {
					failed(e, null);
					return;
				}
			}
		} finally {
			lock.unlock();
		}
		checkWrite();
		long l = lastWriteInit;
		exec.schedule(() -> checkWrite(l), 4, TimeUnit.MILLISECONDS);
	}

	private void checkWrite(long l) {
		if (l == lastWriteInit) {
//			System.out.println("AAAAA");
			checkWrite();
		}

	}
}