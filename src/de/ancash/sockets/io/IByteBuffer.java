package de.ancash.sockets.io;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.ancash.sockets.utils.VarUtils;

public class IByteBuffer {

	private final LinkedList<Byte> buffer = new LinkedList<>();

	public int checkVarInt() {
		return VarUtils.readVarInt(new IByteBuffer().addAll(buffer.subList(0, 5)));
	}

	public long checkVarLong() {
		return VarUtils.readVarLong(new IByteBuffer().addAll(buffer.subList(0, 10)));
	}

	public String checkVarString(int max) {
		return VarUtils.readVarString(new IByteBuffer().addAll(buffer.subList(0, max)));
	}

	public int readVarInt() {
		return VarUtils.readVarInt(this);
	}

	public long readVarLong() {
		return VarUtils.readVarLong(this);
	}

	public String readVarString() {
		return VarUtils.readVarString(this);
	}

	public int readUnsignedShort() {
		return (((pollFirst() & 0xFF) << 8) | (pollFirst() & 0xFF)) & 0xFFFF;
	}

	public List<Byte> subList(int from, int to) {
		return buffer.subList(from, to);
	}

	public IByteBuffer clear() {
		buffer.clear();
		return this;
	}

	public IByteBuffer addFirst(byte b) {
		buffer.addFirst(b);
		return this;
	}

	public IByteBuffer addLast(byte b) {
		buffer.addLast(b);
		return this;
	}

	public IByteBuffer add(int index, byte b) {
		buffer.add(index, b);
		return this;
	}

	public IByteBuffer addAll(byte[] bs) {
		for (byte b : bs)
			buffer.add(b);
		return this;
	}

	public IByteBuffer addAll(Collection<? extends Byte> col) {
		buffer.addAll(col);
		return this;
	}

	public int size() {
		return buffer.size();
	}

	public byte getFirst() {
		return buffer.getFirst();
	}

	public byte getLast() {
		return buffer.getLast();
	}

	public byte get(int i) {
		return buffer.get(i);
	}

	public byte pollFirst() {
		return buffer.pollFirst();
	}

	public byte removeLast() {
		return buffer.removeLast();
	}

	public byte remove(int i) {
		return buffer.remove(i);
	}
}