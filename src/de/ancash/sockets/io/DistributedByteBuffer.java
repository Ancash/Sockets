package de.ancash.sockets.io;

import java.nio.ByteBuffer;

public class DistributedByteBuffer {

	public final int id;
	public final ByteBuffer buffer;
	
	public DistributedByteBuffer(ByteBuffer buffer, int id) {
		this.id = id;
		this.buffer = buffer;
	}
}
