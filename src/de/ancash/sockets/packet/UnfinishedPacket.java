package de.ancash.sockets.packet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class UnfinishedPacket {

	private short header;
	ByteBuffer buffer;

	private final Map<String, Object> someStuff = new HashMap<>();

	public UnfinishedPacket(short header) {
		this.header = header;
	}
	
	public ByteBuffer getBuffer() {
		return buffer;
	}

	public Object remove(String key) {
		return someStuff.remove(key);
	}

	public short getHeader() {
		return header;
	}
}