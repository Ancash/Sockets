package de.ancash.sockets.packet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class UnfinishedPacket {

	private short header;
	ByteBuffer buffer;

	private final Map<String, Object> someStuff = new HashMap<>();

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public Object remove(String key) {
		return someStuff.remove(key);
	}

	public short getHeader() {
		return header;
	}

	public UnfinishedPacket setHeader(short bytesToShort) {
		this.header = bytesToShort;
		return this;
	}
}