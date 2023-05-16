package de.ancash.sockets.async.client;

public abstract class AbstractAsyncReadHandlerFactory {

	public abstract AbstractAsyncReadHandler newInstance(AbstractAsyncClient socket, int readBufSize);

}