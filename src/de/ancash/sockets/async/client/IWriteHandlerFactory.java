package de.ancash.sockets.async.client;

@FunctionalInterface
public interface IWriteHandlerFactory {

	public IWriteHandler newInstance(AbstractAsyncClient socket);
	
}
