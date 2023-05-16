package de.ancash.sockets.packet;

import static de.ancash.misc.ConversionUtil.bytesToInt;
import static de.ancash.misc.ConversionUtil.bytesToShort;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
	private Serializable obj;
	private short header;
	private boolean isClientTarget = true;

	public Packet(short header) {
		this.header = header;
		HEADER_BYTES = ConversionUtil.from(header);
	}

	@Override
	public String toString() {
		return "header=" + header + ", long=" + longValue + ", serializable="
				+ (obj != null ? ReflectionUtils.toString(obj, false) : "null");
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

	public final boolean hasObj() {
		return obj != null;
	}

	public final Serializable getSerializable() {
		return obj;
	}

	public final Packet setSerializable(Serializable value) {
		obj = value;
		return this;
	}

	@Override
	public short getHeader() {
		return header;
	}

	@Override
	public void reconstruct(byte[] bytes) throws IOException {
		if (bytes.length < 15)
			return;
		int size = bytesToInt(Arrays.copyOfRange(bytes, 0, 4));
		this.header = bytesToShort(bytes[4], bytes[5]);
		longValue = ((bytes[6] & 0xFFL) << 56) | ((bytes[7] & 0xFFL) << 48) | ((bytes[8] & 0xFFL) << 40)
				| ((bytes[9] & 0xFFL) << 32) | ((bytes[10] & 0xFFL) << 24) | ((bytes[11] & 0xFFL) << 16)
				| ((bytes[12] & 0xFFL) << 8) | ((bytes[13] & 0xFFL));
		isClientTarget = bytes[14] == 0;
		if (size > 15) {
			byte[] serializableBytes = Arrays.copyOfRange(bytes, 15, bytes.length);
			try {
				obj = (Serializable) SerializationUtils.deserializeWithClassLoaders(serializableBytes);
			} catch (Exception e) {
				throw new IOException("Could not deserialize", e);
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
				e.printStackTrace();
			}
		}
		// length = size + header + isClientTarget + timeStamp + serializable
		int length = 4 + 2 + 1 + 8 + serializedBytes.length;
		bytes = new byte[length];
		bytes[0] = (byte) (length >>> 24);
		bytes[1] = (byte) (length >>> 16);
		bytes[2] = (byte) (length >>> 8);
		bytes[3] = (byte) (length);
		bytes[4] = HEADER_BYTES[1];
		bytes[5] = HEADER_BYTES[0];
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
		return ByteBuffer.wrap(bytes);
	}

	public void resetResponse() {
		response = null;
	}
}