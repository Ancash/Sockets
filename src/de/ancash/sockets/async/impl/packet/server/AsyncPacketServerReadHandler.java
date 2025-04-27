package de.ancash.sockets.async.impl.packet.server;

import de.ancash.sockets.async.client.AbstractAsyncReadHandler;

public class AsyncPacketServerReadHandler extends AbstractAsyncReadHandler {

	public AsyncPacketServerReadHandler(AsyncPacketServer server, AsyncPacketServerClient asyncClient, int readBufSize) {
		super(asyncClient, readBufSize, null);
	}

	@Override
	public void onDisconnect() {

	}
}