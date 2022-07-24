package de.ancash.sockets.async.client;

public abstract class AbstractAsyncWriteHandlerFactory {

	public abstract AbstractAsyncWriteHandler newInstance(AbstractAsyncClient socket) ;
	
}