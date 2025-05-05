package de.ancash.sockets.async.server;

@FunctionalInterface
public interface IAcceptHandlerFactory {

	public DefaultAsyncAcceptHandler newInstance(AbstractAsyncServer server);

}