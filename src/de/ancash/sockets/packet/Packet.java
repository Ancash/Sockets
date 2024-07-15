package de.ancash.sockets.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.ancash.misc.ConversionUtil;
import de.ancash.misc.ReflectionUtils;
import de.ancash.misc.io.SerializationUtils;

public class Packet implements PacketInterface, Serializable, Cloneable {

	private static final long serialVersionUID = 962998206232520827L;

	public static final short PING_PONG = 32723;

	private byte[] HEADER_BYTES;

	private transient PacketCallback packetCallback;
	private transient boolean await = false;
	private transient Packet response = null;
	private transient Object awaitObject = new Object();

	private long longValue = 0L;
	private Object obj;
	private short header;
	private boolean isClientTarget = true;

	public Packet(short header) {
		this.header = header;
		HEADER_BYTES = SerializationUtil.shortToBytes(header);
	}

	@Override
	public String toString() {
		return "header=" + header + ", long=" + longValue + ", serializable=" + (obj != null ? ReflectionUtils.toString(obj, false) : "null");
	}

	@Override
	public Object clone() {
		Packet clone = new Packet(getHeader());
		clone.longValue = longValue;
		clone.obj = obj;
		return clone;
	}

	public void setAwaitResponse(boolean b) {
		await = b;
	}

	public Packet getResponse() {
		return response;
	}

	public Optional<Packet> awaitResponse(long millis) throws InterruptedException {
		synchronized (awaitObject) {
			synchronized (this) {
				if (response != null)
					return Optional.of(response);
			}
			awaitObject.wait(millis);
			return Optional.ofNullable(response);
		}
	}

	public void awake(Packet packet) {
		synchronized (awaitObject) {
			synchronized (this) {
				this.response = packet;
				awaitObject.notify();
			}
		}
	}

	public boolean isAwaitingRespose() {
		return await;
	}

	public void setPacketCallback(PacketCallback pc) {
		this.packetCallback = pc;
	}

	public boolean hasPacketCallback() {
		return packetCallback != null;
	}

	public PacketCallback getPacketCallback() {
		return packetCallback;
	}

	public final boolean isClientTarget() {
		return isClientTarget;
	}

	public final void isClientTarget(boolean isClientTarget) {
		this.isClientTarget = isClientTarget;
	}

	public final long getTimeStamp() {
		return longValue;
	}

	public Packet setLong(long l) {
		this.longValue = l;
		return this;
	}

	public final void addTimeStamp() {
		longValue = System.nanoTime();
	}

	public final boolean hasObject() {
		return obj != null;
	}

	public final Object getObject() {
		return obj;
	}

	public final Packet setObject(Object value) {
		obj = value;
		return this;
	}

	@Override
	public short getHeader() {
		return header;
	}

	@Override
	public void reconstruct(ByteBuffer buffer) throws IOException {
		if (buffer.limit() < 15)
			return;
		byte[] temp = new byte[4];
		buffer.get(temp);
		int size = SerializationUtil.bytesToInt(temp);
		temp = new byte[2];
		buffer.get(temp);
		this.header = SerializationUtil.bytesToShort(temp);
		longValue = ((buffer.get() & 0xFFL) << 56) | ((buffer.get() & 0xFFL) << 48) | ((buffer.get() & 0xFFL) << 40) | ((buffer.get() & 0xFFL) << 32)
				| ((buffer.get() & 0xFFL) << 24) | ((buffer.get() & 0xFFL) << 16) | ((buffer.get() & 0xFFL) << 8) | ((buffer.get() & 0xFFL));
		isClientTarget = buffer.get() == 0;
		if (size > 15) {
			temp = new byte[buffer.remaining()];
			buffer.get(temp);
			try {
				obj = SerializationUtils.deserializeWithClassLoaders(temp);
			} catch (ClassNotFoundException | IOException e) {
				throw new IllegalStateException(e);

			}
//			obj = SerializationUtils.deserializeFST(temp);
		}
	}

	@SuppressWarnings("nls")
	@Override
	public ByteBuffer toBytes() {
		byte[] serializedBytes = null;
		boolean deserialize = false;
		if (obj == null) {
			serializedBytes = new byte[0];
		} else {
			try {
				serializedBytes = SerializationUtils.serializeToBytes(obj);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
//			serializedBytes = SerializationUtils.serializeFST(obj);
		}
		byte flags = 0;
		if (!isClientTarget)
			flags = setBit(flags, 0);
		if (deserialize)
			flags = setBit(flags, 1);

		// length = size + header + isClientTarget + timeStamp + serializable
		int length = 4 + 2 + 1 + 8 + serializedBytes.length;
		ByteBuffer finalBB = ByteBuffer.allocateDirect(length);
		finalBB.put((byte) (length >>> 24));
		finalBB.put((byte) (length >>> 16));
		finalBB.put((byte) (length >>> 8));
		finalBB.put((byte) length);
		finalBB.put(HEADER_BYTES[0]);
		finalBB.put(HEADER_BYTES[1]);
		finalBB.put((byte) (longValue >>> 56));
		finalBB.put((byte) (longValue >>> 48));
		finalBB.put((byte) (longValue >>> 40));
		finalBB.put((byte) (longValue >>> 32));
		finalBB.put((byte) (longValue >>> 24));
		finalBB.put((byte) (longValue >>> 16));
		finalBB.put((byte) (longValue >>> 8));
		finalBB.put((byte) (longValue));
		finalBB.put(flags);
		finalBB.put(serializedBytes);
		finalBB.position(0);
		return finalBB;
	}

	public static byte setBit(byte b, int pos) {
		return b |= 1 << pos;
	}

	public static byte unsetBit(byte b, int pos) {
		return b &= ~(1 << pos);
	}

	public void resetResponse() {
		response = null;
	}
}