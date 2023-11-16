package de.ancash.sockets.async;

import java.nio.ByteBuffer;

public interface ByteEventHandler {

	public void onBytes(ByteBuffer bb);

}