package de.ancash.sockets.packet;

import java.util.HashMap;
import java.util.Map;

import de.ancash.sockets.io.PositionedByteBuf;

public class UnfinishedPacket {

	private short header;
	PositionedByteBuf buffer;
	
	private final Map<String, Object> someStuff = new HashMap<>();

	public PositionedByteBuf getBuffer() {
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