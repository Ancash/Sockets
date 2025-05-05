package de.ancash.sockets.async.client;

@FunctionalInterface
public interface IReadHandlerFactory {

	public DefaultAsyncReadHandler newInstance(AbstractAsyncClient cl);
	
}
