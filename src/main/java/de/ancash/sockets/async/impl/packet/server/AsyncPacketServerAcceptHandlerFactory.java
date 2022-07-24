package de.ancash.sockets.async.impl.packet.server;

import de.ancash.sockets.async.server.AbstractAsyncAcceptHandlerFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;

public class AsyncPacketServerAcceptHandlerFactory extends AbstractAsyncAcceptHandlerFactory{

	@Override
	public AsyncPacketServerAcceptHandler newInstance(AbstractAsyncServer server) {
		return new AsyncPacketServerAcceptHandler(server);
	}
}