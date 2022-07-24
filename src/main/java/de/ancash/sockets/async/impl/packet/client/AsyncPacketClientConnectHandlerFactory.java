package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncConnectHandler;
import de.ancash.sockets.async.client.AbstractAsyncConnectHandlerFactory;

public class AsyncPacketClientConnectHandlerFactory extends AbstractAsyncConnectHandlerFactory{

	@Override
	public AbstractAsyncConnectHandler newInstance(AbstractAsyncClient client) {
		return new AsyncPacketClientConnectHandler(client);
	}
}