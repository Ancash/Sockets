package de.ancash.sockets.async.server;

public abstract class AbstractAsyncAcceptHandlerFactory {

	public abstract AbstractAsyncAcceptHandler newInstance(AbstractAsyncServer server);

}