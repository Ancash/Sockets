package de.ancash.sockets.async.client;

public abstract class AbstractAsyncConnectHandlerFactory {

	public abstract AbstractAsyncConnectHandler newInstance(AbstractAsyncClient client);
	
}