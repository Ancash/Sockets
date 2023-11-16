package de.ancash.sockets.io;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import de.ancash.Sockets;

public class ByteBufferDistributor {

	public static void main(String[] args) throws InterruptedException {
		ByteBufferDistributor fbb = new ByteBufferDistributor(1024 * 16, 128);
		new Thread(() -> {
			while (true) {
				PositionedByteBuf pbb = fbb.getAvailableBuffer();
				fbb.unblockBuffer(pbb);
				System.out.println(fbb);
			}
		}).start();
	}

	public final PositionedByteBuf[] buffers;
	private final AtomicBoolean readLock = new AtomicBoolean(false);
	private final AtomicBoolean[] locked;

	public ByteBufferDistributor(int bufSize, int buffers) {
		locked = new AtomicBoolean[buffers];
		for (int a = 0; a < buffers; a++)
			locked[a] = new AtomicBoolean(false);
		this.buffers = new PositionedByteBuf[buffers];
		for (int i = 0; i < buffers; i++)
			this.buffers[i] = new PositionedByteBuf(ByteBuffer.allocateDirect(bufSize), i);
	}

	public int capacity() {
		return buffers.length;
	}

	public int getNumAvailableBuffers() {
		return (int) IntStream.range(0, buffers.length).boxed().filter(i -> !locked[i].get()).count();
	}

	public boolean isBufferAvailable() {
		return Arrays.asList(locked).stream().map(AtomicBoolean::get).filter(b -> !b).findAny().isPresent();
	}

	public AtomicInteger blockCnt = new AtomicInteger();

	public void unblockBuffer(PositionedByteBuf bb) {
		if (locked[bb.getAId()].get()) {
			bb.owner = null;
			blockCnt.decrementAndGet();
			bb.get().position(0);
			bb.get().limit(bb.get().capacity());
			bb.get().mark();
			if (!locked[bb.getAId()].compareAndSet(true, false))
				throw new IllegalArgumentException("cannot double unblock buffer");
		} else
			throw new IllegalArgumentException("cannot double unblock buffer");

	}

	public PositionedByteBuf getAvailableBuffer() {
		try {
			while (!readLock.compareAndSet(false, true))
				Sockets.sleep(10_000);
			if (!isBufferAvailable())
				return null;
			PositionedByteBuf pbb = getAvailableBuffer0();
			return pbb;
		} finally {
			readLock.set(false);
		}
	}

	private PositionedByteBuf getAvailableBuffer0() {
		int i = 0;
		for (; i < buffers.length; i++)
			if (!locked[i].get())
				break;
		PositionedByteBuf buf = buffers[i];
		if (!locked[i].compareAndSet(false, true))
			throw new IllegalStateException("buffer already locked");
		buf.get().position(0);
		buf.get().limit(buf.get().capacity());
		buf.get().mark();
		blockCnt.incrementAndGet();
		return buf;
	}
}
