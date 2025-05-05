package de.ancash.sockets.async.client;

@FunctionalInterface
public interface AbstractAsyncConnectHandlerFactory {

	public DefaultAsyncConnectHandler newInstance(AbstractAsyncClient client);

}