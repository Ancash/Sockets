package de.ancash.sockets.packet;

import java.util.HashMap;
import java.util.Map;

public class UnfinishedPacket {

	private byte[] bytes;
	private short header;
	
	private final Map<String, Object> someStuff = new HashMap<>();
	
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public UnfinishedPacket setBytes(byte[] bytes) {
		this.bytes = bytes;
		return this;
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