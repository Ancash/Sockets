package de.ancash.sockets.async.client;

public abstract class AbstractAsyncWriteHandlerFactory {

	public abstract AbstractAsyncByteBufWriteHandler newInstance(AbstractAsyncClient socket);

}