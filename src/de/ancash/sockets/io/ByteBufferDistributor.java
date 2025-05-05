package de.ancash.sockets.io;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ByteBufferDistributor {

	private static final AtomicInteger idCnt = new AtomicInteger();
	
	private final LinkedBlockingQueue<DistributedByteBuffer> availableBuffers;
	private final int id = idCnt.getAndIncrement();
	
	public ByteBufferDistributor(int bufSize, int buffers) {
		this.availableBuffers = new LinkedBlockingQueue<DistributedByteBuffer>(bufSize);
		for (int i = 0; i < buffers; i++)
			availableBuffers.add(new DistributedByteBuffer(ByteBuffer.allocateDirect(bufSize), id));
	}

	public int getNumAvailableBuffers() {
		return availableBuffers.size();
	}

	public boolean isBufferAvailable() {
		return !availableBuffers.isEmpty();
	}

	public boolean freeBuffer(DistributedByteBuffer bb) {
		if(bb.id != id) {
			return false;
		}
		bb.buffer.clear();
		availableBuffers.add(bb);
		return true;
	}

	public DistributedByteBuffer getBuffer() {
		return availableBuffers.poll();
	}

	public DistributedByteBuffer getBufferBlocking(long timeout, TimeUnit unit)  {
		try {
			return availableBuffers.poll(timeout, unit);
		} catch (InterruptedException e) {
			return null;
		}
	}
}
