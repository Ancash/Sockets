package de.ancash.sockets.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import de.ancash.Sockets;

public class FixedByteBuffer {

	public static void main(String[] args) throws InterruptedException {
		FixedByteBuffer fbb = new FixedByteBuffer(1024 * 16, 128, 1000000);
		new Thread(() -> {
			byte[] bb = new byte[1024 * 64];
			while (true) {
//				fbb.put(bb);
			}
		}).start();
		long cnt = 0;
		long now = System.nanoTime();
		while (true) {
			try {
				while (!fbb.canRead())
					LockSupport.parkNanos(10_000);
				fbb.unblock(fbb.read());
				if (cnt++ % 1000 == 0)
					System.out.println(cnt / ((System.nanoTime() - now) / 1000000000D) + " bufs/sec");
			} catch (Throwable th) {
				th.printStackTrace();
			}
		}
	}

	protected final ByteBuffer[] buffers;
	private final AtomicInteger writeBufPos = new AtomicInteger();
	private final AtomicInteger readBufPos = new AtomicInteger();
	private final int bufSize;
	private final AtomicBoolean readLock = new AtomicBoolean(false);
	private final AtomicBoolean[] locked;
	private final LinkedBlockingQueue<ByteBuffer> overflow;
	private final AtomicReference<Long> testLock = new AtomicReference<>(null);
	public Runnable r;

	public FixedByteBuffer(int bufSize, int buffers, int queueSize) {
		overflow = new LinkedBlockingQueue<>(queueSize);
		buffers++;
		if (buffers < 3)
			throw new IllegalArgumentException("arr size 3 min");
		this.bufSize = bufSize;
		locked = new AtomicBoolean[buffers];
		for (int a = 0; a < buffers; a++)
			locked[a] = new AtomicBoolean(false);
		this.buffers = new ByteBuffer[buffers];
		for (int i = 0; i < buffers; i++)
			this.buffers[i] = ByteBuffer.allocateDirect(bufSize);
	}

	public boolean canRead() {
		return writeBufPos.get() != readBufPos.get();
	}

	public void unblock(PositionedByteBuf bb) {
		if (!locked[bb.getAId()].compareAndSet(true, false))
			throw new IllegalArgumentException("cannot double unblock buffer");
		checkOverflow();
	}

	public PositionedByteBuf read() {
		try {
			while (!readLock.compareAndSet(false, true))
				Sockets.sleep(10_000);
			if (!canRead())
				return null;
			PositionedByteBuf pbb = read0();
			return pbb;
		} finally {
			readLock.set(false);
		}
	}

	private PositionedByteBuf read0() {
		ByteBuffer buf = buffers[readBufPos.get()];
		if (!locked[readBufPos.get()].get())
			throw new IllegalStateException("buffer not locked");
		PositionedByteBuf pbb = new PositionedByteBuf(buf, readBufPos.get());
		readBufPos.set((readBufPos.get() + 1) % buffers.length);
		return pbb;
	}

	private void checkOverflow() {
		if (!canWrite())
			return;
		boolean replace = false;
		try {
			if (Optional.ofNullable(testLock.get()).orElse(Thread.currentThread().getId() + 1) == Thread.currentThread()
					.getId())
				replace = false;
			else if (!testLock.compareAndSet(null, Thread.currentThread().getId()))
				return;
			else
				replace = true;
			while (canWrite() && !overflow.isEmpty())
				put0(overflow.poll());
		} finally {
			if (replace)
				testLock.set(null);
		}
	}

	public boolean canWrite() {
		return !locked[writeBufPos.get()].get() && (writeBufPos.get() + 1) % buffers.length != readBufPos.get();
	}

	public void put(ByteBuffer bb) {
		List<ByteBuffer> list = new ArrayList<>();
		Iterator<ByteBuffer> iter;

		try {
			while (!testLock.compareAndSet(null, Thread.currentThread().getId()))
				Sockets.sleep(10_000);
			while (bb.hasRemaining()) {
				ByteBuffer dup = bb.slice();
				dup.limit(Math.min(bb.remaining(), bufSize));
				bb.position(bb.position() + Math.min(bb.remaining(), bufSize));
				list.add(dup);
			}
			iter = list.iterator();
			if (overflow.isEmpty()) {
				while (iter.hasNext() && canWrite()) {
					put0(iter.next());
				}
			}
			long l = System.currentTimeMillis() + 1000;
			while (iter.hasNext()) {
				if (canWrite() && !overflow.isEmpty()) {
					checkOverflow();
				}
				if (overflow.remainingCapacity() > 0)
					overflow.add(iter.next());
				else {
					if (l < System.currentTimeMillis()) {
						if (r != null)
							r.run();
						l += 1000;
					}
					Sockets.sleep(10_000);
				}
			}
		} finally {
			testLock.set(null);
		}
	}

	private void put0(ByteBuffer b) {
		if (b.remaining() > bufSize)
			throw new IllegalArgumentException("buffer of size " + b.remaining() + " does not fit into " + bufSize);
		if (!locked[writeBufPos.get()].compareAndSet(false, true)) {
			throw new IllegalStateException("could not lock buf " + writeBufPos.get());
		}
		ByteBuffer cur = buffers[writeBufPos.get()];
		cur.position(cur.capacity() - b.remaining());
		cur.mark();
		cur.put(b);
		cur.reset();
		writeBufPos.set((writeBufPos.get() + 1) % buffers.length);
	}
}
