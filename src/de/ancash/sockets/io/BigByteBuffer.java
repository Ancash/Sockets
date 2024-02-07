package de.ancash.sockets.io;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import de.ancash.Sockets;

public class BigByteBuffer {

	public static void main(String[] args) {
		BigByteBuffer bbb = new BigByteBuffer(ByteSizeConstants._128, ByteSizeConstants._1024x256, 8);
		int cnt = 0;
		long l = System.nanoTime();
		while (true) {
			bbb.unblockBuffer(bbb.blockBuffer(12345));
			cnt++;
			if (cnt % 1000 == 0)
				System.out.println(cnt / ((System.nanoTime() - l) / 1_000_000_000D) + " bufs/sec");
		}

	}

	private final ByteSizeConstants from, to;
	private final int maxSize, minSize;
	private final int ordinalOffset;
	private final PositionedByteBuf[][] buffers;
	private final int[] sizes;
	private final AtomicBoolean[][] blocked;
//	private final AtomicReference<Long>[] locks;
	final AtomicReference<Long> locks = new AtomicReference<Long>(null);

	@SuppressWarnings("unchecked")
	public BigByteBuffer(ByteSizeConstants from, ByteSizeConstants to, int minSize) {
		if (from.ordinal() > to.ordinal())
			throw new IllegalArgumentException(from + " > " + to);
		this.ordinalOffset = from.ordinal();
		this.from = from;
		this.to = to;
		this.maxSize = (int) (minSize * Math.pow(2, to.ordinal() - from.ordinal()));
		this.minSize = minSize;
		this.buffers = new PositionedByteBuf[to.ordinal() - from.ordinal() + 1][];
		this.sizes = new int[to.ordinal() - from.ordinal() + 1];
		this.blocked = new AtomicBoolean[to.ordinal() - from.ordinal() + 1][];
//		this.locks = new AtomicReference[to.ordinal() - from.ordinal() + 1];
		int sum = 0;
		for (int i = 0; i < sizes.length; i++) {
			sizes[i] = (int) (minSize * Math.pow(2, to.ordinal() - from.ordinal() - i));
			buffers[i] = new PositionedByteBuf[sizes[i]];
			blocked[i] = new AtomicBoolean[sizes[i]];
//			locks[i] = new AtomicReference<Long>(null);
			ByteSizeConstants size = ByteSizeConstants.values()[ordinalOffset + i];
			for (int b = 0; b < buffers[i].length; b++) {
				buffers[i][b] = new PositionedByteBuf(ByteBuffer.allocateDirect(size.getSize()), i, b);
				blocked[i][b] = new AtomicBoolean(false);
			}
			System.out.println("allocated " + sizes[i] + " x " + size);
			sum += sizes[i] * size.getSize();
		}
		System.out.println("total " + sum);
	}

	private boolean isBiggerAvailable(ByteSizeConstants bsc) {
		int start = bsc.ordinal() - ordinalOffset;
		for (int i = start; i < blocked[start].length; i++) {
			if (isAvailable(ByteSizeConstants.values()[ordinalOffset + i]))
				return true;
		}
		return false;
	}

	public boolean isAvailable(ByteSizeConstants bsc) {
		int arrPos = Math.max(bsc.ordinal() - ordinalOffset, 0);
		for (int i = 0; i < blocked[arrPos].length; i++)
			if (!blocked[arrPos][i].get())
				return true;
		return false;
	}

	public PositionedByteBuf blockBuffer(int size) {
		ByteSizeConstants bsc = ByteSizeConstants._1;
		while (bsc.getSize() < size)
			bsc = ByteSizeConstants.values()[bsc.ordinal() + 1];
		int arrPos = Math.max(bsc.ordinal() - ordinalOffset, 0);
		bsc = ByteSizeConstants.values()[arrPos + ordinalOffset];
//		System.out.println(size + "->" + bsc + "-> " + arrPos);
//		AtomicReference<Long> lock = locks[arrPos];
		long tid = Thread.currentThread().getId();
		try {
			while (!locks.compareAndSet(null, tid) && !locks.compareAndSet(tid, tid))
				Sockets.sleepMillis(1);
			long l = System.currentTimeMillis() + 1000;
			while (!isBiggerAvailable(bsc)) {
				Sockets.sleepMillis(1);
				if (l < System.currentTimeMillis()) {
					System.out.println("no bufs av");
					l = System.currentTimeMillis() + 1000;
				}
			}

			int bufPos = 0;
			PositionedByteBuf[] bufs = buffers[arrPos];
			AtomicBoolean[] ls = blocked[arrPos];
			for (; bufPos < bufs.length; bufPos++)
				if (ls[bufPos].compareAndSet(false, true))
					break;

			if (bufPos == bufs.length) {
				Sockets.sleepMillis(1);
				return blockBuffer(bsc.getSize());
			}
//			System.out.println("blocked " + bufPos);
			PositionedByteBuf res = bufs[bufPos];
			res.get().position(0);
			res.get().limit(res.get().capacity());
			res.get().mark();
			return res;
		} finally {
			locks.compareAndSet(tid, null);
		}
	}

	public void unblockBuffer(PositionedByteBuf pbb) {
		if (!blocked[pbb.getAId()][pbb.getBId()].compareAndSet(true, false))
			throw new IllegalArgumentException("cannot unblock free buffer");
	}
}
