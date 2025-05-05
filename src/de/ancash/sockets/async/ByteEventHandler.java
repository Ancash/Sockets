package de.ancash.sockets.async;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface ByteEventHandler {

	public void onBytes(ByteBuffer bb);

}