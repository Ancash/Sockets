package de.ancash.sockets.async.client;

public abstract class WriteHandlerFactory {

	public abstract IWriteHandler newInstance(AbstractAsyncClient socket);

}