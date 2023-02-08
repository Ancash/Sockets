package de.ancash.sockets.async.impl.packet.server;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncReadHandler;
import de.ancash.sockets.async.client.AbstractAsyncReadHandlerFactory;

public class AsyncPacketServerReadHandlerFactory extends AbstractAsyncReadHandlerFactory{

	private final AsyncPacketServer server;
	
	public AsyncPacketServerReadHandlerFactory(AsyncPacketServer server) {
		this.server = server;
	}

	@Override
	public AbstractAsyncReadHandler newInstance(AbstractAsyncClient socket, int readBufSize) {
		return new AsyncPacketServerReadHandler(server, (AsyncPacketServerClient) socket, readBufSize);
	}
}