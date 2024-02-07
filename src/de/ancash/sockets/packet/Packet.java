package de.ancash.sockets.packet;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Optional;

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
				throw new IOException(e);
			}
		}
	}

	@Override
	public ByteBuffer toBytes() {
		byte[] bytes;
		byte[] serializedBytes = null;
		if (obj == null) {
			serializedBytes = new byte[0];
		} else {
			try {
				serializedBytes = SerializationUtils.serializeToBytes(obj);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		// length = size + header + isClientTarget + timeStamp + serializable
		int length = 4 + 2 + 1 + 8 + serializedBytes.length;
		bytes = new byte[length];
		bytes[0] = (byte) (length >>> 24);
		bytes[1] = (byte) (length >>> 16);
		bytes[2] = (byte) (length >>> 8);
		bytes[3] = (byte) (length);
		bytes[4] = HEADER_BYTES[0];
		bytes[5] = HEADER_BYTES[1];
		bytes[6] = (byte) (longValue >>> 56);
		bytes[7] = (byte) (longValue >>> 48);
		bytes[8] = (byte) (longValue >>> 40);
		bytes[9] = (byte) (longValue >>> 32);
		bytes[10] = (byte) (longValue >>> 24);
		bytes[11] = (byte) (longValue >>> 16);
		bytes[12] = (byte) (longValue >>> 8);
		bytes[13] = (byte) (longValue);
		bytes[14] = (byte) (isClientTarget ? 0 : 1);
		System.arraycopy(serializedBytes, 0, bytes, 15, serializedBytes.length);
		ByteBuffer bb = ByteBuffer.allocate(bytes.length);
		bb.put(bytes);
		bb.position(0);
		bb.limit(bytes.length);
		bb.mark();
		return bb;
	}

	public void resetResponse() {
		response = null;
	}
}