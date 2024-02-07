package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncByteBufWriteHandler;
import de.ancash.sockets.async.client.AbstractAsyncClient;

public class AsyncPacketClientWriteHandler extends AbstractAsyncByteBufWriteHandler {

	public AsyncPacketClientWriteHandler(AbstractAsyncClient asyncClient) {
		super(asyncClient);
	}
}