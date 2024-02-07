package de.ancash.sockets.io;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.ancash.Sockets;
import de.ancash.libs.org.apache.commons.lang3.Validate;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCombiner;

public class FixedByteBuffer {

	public static void main(String[] args) throws InterruptedException {
		FixedByteBuffer fbb = new FixedByteBuffer(1024 * 32, 128);
		Packet packet = new Packet(Packet.PING_PONG);
		int pl = 1000;
		packet.setObject(new byte[pl]);
		int size = packet.toBytes().remaining();
		int f = 2048;
		byte[] bb = new byte[pl];
		packet.setObject(bb);
		int cnt = 0;
		for (int i = 0; i < 10; i++) {
			fbb.put(packet.toBytes());
		}
		PacketCombiner pc = new PacketCombiner(1024 * 1024);
		while (fbb.canRead()) {
			PositionedByteBuf pbb = fbb.read();
			System.out.println(pbb.get());
			System.out.println(pc.put(pbb.get()));
			fbb.unblock(pbb);
		}
	}

	protected final PositionedByteBuf[] buffers;
	private final AtomicInteger writeBufPos = new AtomicInteger();
	private final AtomicInteger readBufPos = new AtomicInteger();
	private final AtomicLong readLock = new AtomicLong(-1);
	private final AtomicBoolean[] locked;
	private final AtomicBoolean[] using;
	private final AtomicReference<Long> writeLock = new AtomicReference<>(null);
	public Runnable r;

	@SuppressWarnings("nls")
	public FixedByteBuffer(int bufSize, int buffers) {
		buffers++;
		if (buffers < 3)
			throw new IllegalArgumentException("arr size 3 min");
		locked = new AtomicBoolean[buffers];
		using = new AtomicBoolean[buffers];
		for (int a = 0; a < buffers; a++) {
			locked[a] = new AtomicBoolean(false);
			using[a] = new AtomicBoolean(false);
		}
		this.buffers = new PositionedByteBuf[buffers];
		for (int i = 0; i < buffers; i++)
			this.buffers[i] = new PositionedByteBuf(ByteBuffer.allocateDirect(bufSize), i);
	}

	public boolean canRead() {
		return writeBufPos.get() != readBufPos.get();
	}

	@SuppressWarnings("nls")
	public void unblock(PositionedByteBuf bb) {
		if (!locked[bb.getAId()].compareAndSet(true, false) || !using[bb.getAId()].compareAndSet(true, false))
			throw new IllegalArgumentException("cannot double unblock buffer");
	}

	public PositionedByteBuf read() {
		try {
			while (!readLock.compareAndSet(-1, Thread.currentThread().getId()))
				Sockets.sleepMillis(1);
			if (!canRead())
				return null;
			PositionedByteBuf pbb = read0();
			if (pbb.get().remaining() == 0) {
				unblock(pbb);
				return read();
			}
			return pbb;
		} finally {
			readLock.set(-1);
		}
	}

	@SuppressWarnings("nls")
	private PositionedByteBuf read0() {
		PositionedByteBuf buf = buffers[readBufPos.get()];
		if (!locked[readBufPos.get()].get())
			throw new IllegalStateException("buffer not locked");
		using[readBufPos.get()].set(true);
		readBufPos.set((readBufPos.get() + 1) % buffers.length);
		return buf;
	}

	public boolean canWrite() {
		return !locked[writeBufPos.get()].get() && (writeBufPos.get() + 1) % buffers.length != readBufPos.get();
	}

	public void put(ByteBuffer bb) {
		try {
			while (!writeLock.compareAndSet(null, Thread.currentThread().getId()))
				Sockets.sleepMillis(1);
			put0(bb);
		} finally {
			writeLock.set(null);
		}
	}

	@SuppressWarnings("nls")
	private void put0(ByteBuffer source) {
		int cnt = 0;
		while (!canWrite()) {
			Sockets.sleepMillis(1);
			if (cnt++ % 5 == 0 && r != null)
				r.run();
		}

		try {

			while (!readLock.compareAndSet(-1, Thread.currentThread().getId()))
				Sockets.sleepMillis(1);
			int prev = (writeBufPos.get() + buffers.length - 1) % buffers.length;
			ByteBuffer target = buffers[prev].get();
			int free = target.capacity() - target.limit();

			if (free >= source.remaining() && locked[prev].get() && !using[prev].get()) {

				int useable = Math.min(free, source.remaining());
				if (useable > 0) {
					int oldLimit = source.limit();
					target.position(target.limit());
					target.limit(target.limit() + useable);
					source.limit(source.position() + useable);
					target.put(source);
					source.limit(oldLimit);
					target.flip();
					if (!source.hasRemaining())
						return;
//						else
//							System.out.println("rem " + source);
				}
			}

		} finally {
			Validate.isTrue(readLock.compareAndSet(Thread.currentThread().getId(), -1));
		}

		if (!locked[writeBufPos.get()].compareAndSet(false, true)) {
			throw new IllegalStateException("could not lock buf " + writeBufPos.get());
		}

		PositionedByteBuf cur = buffers[writeBufPos.get()];
		cur.get().clear();
		int oldLimit = source.limit();
		int useable = Math.min(source.remaining(), cur.get().remaining());
		source.limit(source.position() + useable);
		cur.get().put(source);
		cur.get().flip();
		source.limit(oldLimit);
		writeBufPos.set((writeBufPos.get() + 1) % buffers.length);
		if (source.hasRemaining())
			put0(source);
	}
}
