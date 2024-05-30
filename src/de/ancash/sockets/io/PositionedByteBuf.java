package de.ancash.sockets.io;

import java.nio.ByteBuffer;

public class PositionedByteBuf {

	final ByteBuffer bb;
	private final int a;
	private final int b;

	public PositionedByteBuf(ByteBuffer bb, int a) {
		this(bb, a, -1);
	}

	public PositionedByteBuf(ByteBuffer bb, int a, int b) {
		this.bb = bb;
		this.a = a;
		this.b = b;
	}

	public ByteBuffer get() {
		return bb;
	}

	public int getAId() {
		return a;
	}

	public int getBId() {
		return b;
	}
}
