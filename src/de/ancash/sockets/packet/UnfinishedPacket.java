package de.ancash.sockets.packet;

import java.util.HashMap;
import java.util.Map;

import de.ancash.sockets.io.DistributedByteBuffer;

public class UnfinishedPacket {

	private short header;
	public DistributedByteBuffer buffer;
	public byte[] content;
	
	private final Map<String, Object> someStuff = new HashMap<>();

	public UnfinishedPacket(short header) {
		this.header = header;
	}

	public Object remove(String key) {
		return someStuff.remove(key);
	}

	public short getHeader() {
		return header;
	}
}