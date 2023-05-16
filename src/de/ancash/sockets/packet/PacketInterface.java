package de.ancash.sockets.packet;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface PacketInterface {

	/**
	 * Get the header of this packet
	 *
	 * @return The header of this packet, 2 bytes long
	 */
	public short getHeader();

	/**
	 * Reconstruct this PacketInterface from the given ByteBuffer
	 *
	 * @param source The ByteBuffer to reconstruct this PacketInterface from
	 */
	public void reconstruct(byte[] bytes) throws IOException;

	/**
	 * Get the contents of this PacketInterface as a ByteBuffer
	 *
	 * @return the contents of this PacketInterface as a ByteBuffer
	 */
	public ByteBuffer toBytes();
}
