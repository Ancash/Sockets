package de.ancash.sockets.io;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

public class RotatingBuffer {

	public static void main(String[] args) throws InterruptedException {
		RotatingBuffer buf = new RotatingBuffer(1024, 4);
		int i = 116;
		byte[] bs = new byte[i];
		for (byte b = 0; b < i; b++) {
			bs[b] = b;
		}
		ByteBuffer bb = ByteBuffer.wrap(bs);
		buf.write(bb);
		System.out.println(buf.read());

	}

	private final ByteBuffer[] bufs;
	private LinkedBlockingDeque<DistributedByteBuffer> availableBufs;
	private LinkedBlockingDeque<DistributedByteBuffer> readyForReadBufs;
	private volatile Thread threadTakingAvailable;

	public RotatingBuffer(int bufSize, int cnt) {
		bufs = new ByteBuffer[cnt];
		availableBufs = new LinkedBlockingDeque<DistributedByteBuffer>(cnt);
		readyForReadBufs = new LinkedBlockingDeque<DistributedByteBuffer>(cnt);
		for (int i = 0; i < cnt; i++) {
			bufs[i] = ByteBuffer.allocateDirect(bufSize);
			availableBufs.add(new DistributedByteBuffer(bufs[i], i));
		}
	}

	public boolean canRead() {
		return !readyForReadBufs.isEmpty();
	}
	
	public void free(DistributedByteBuffer buf) {
		if (!buf.buffer.isDirect())
			return;
		buf.buffer.clear();
		availableBufs.add(buf);
	}

	public DistributedByteBuffer read() {
		DistributedByteBuffer b = readyForReadBufs.poll();
		if (b != null) {
			b.buffer.limit(b.buffer.position());
			b.buffer.position(0);
		}
		return b;
	}
	
	public void close() {
		if(threadTakingAvailable != null) {
			try {
				threadTakingAvailable.interrupt();
			} catch(Throwable th) {
				
			}
			threadTakingAvailable = null;
			availableBufs = null;
			readyForReadBufs = null;
		}
	}

	public synchronized int writeBlocking(ByteBuffer src) throws InterruptedException {
		if (readyForReadBufs.remainingCapacity() == 0) {
			return 0;
		}
		DistributedByteBuffer target = readyForReadBufs.pollLast();
		if (target == null || !target.buffer.hasRemaining()) {
			if (target != null) {
				readyForReadBufs.add(target);
			}
			threadTakingAvailable = Thread.currentThread();
			target = availableBufs.take();
			threadTakingAvailable = null;
		}
		return write(src, target);
	}
	
	private int write(ByteBuffer src, DistributedByteBuffer target) throws InterruptedException {
		int oldLimit = src.limit();
		src.limit(Math.min(src.limit(), src.position() + target.buffer.remaining()));
		int written = src.remaining();
		target.buffer.put(src);
		src.limit(oldLimit);
		readyForReadBufs.add(target);
		if (src.hasRemaining()) {
			return written + write(src);
		}
		return written;
	}

	public synchronized int write(ByteBuffer src) throws InterruptedException {
		if (readyForReadBufs.remainingCapacity() == 0) {
			return 0;
		}
		DistributedByteBuffer target = readyForReadBufs.pollLast();
		if (target == null || !target.buffer.hasRemaining()) {
			if (target != null) {
				readyForReadBufs.add(target);
			}

			target = availableBufs.poll();
			if (target == null) {
				return 0;
			}
		}
		return write(src, target);
	}
}
