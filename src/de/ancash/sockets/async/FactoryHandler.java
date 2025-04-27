package de.ancash.sockets.async;

import de.ancash.sockets.async.client.AbstractAsyncClientFactory;
import de.ancash.sockets.async.client.AbstractAsyncConnectHandlerFactory;
import de.ancash.sockets.async.client.AbstractAsyncReadHandlerFactory;
import de.ancash.sockets.async.client.WriteHandlerFactory;
import de.ancash.sockets.async.server.AbstractAsyncAcceptHandlerFactory;

public class FactoryHandler {

	private AbstractAsyncAcceptHandlerFactory acceptHandlerFactory;
	private AbstractAsyncReadHandlerFactory readHandlerFactory;
	private WriteHandlerFactory writeHandlerFactory;
	private AbstractAsyncConnectHandlerFactory connectHandlerFactory;
	@SuppressWarnings("rawtypes")
	private AbstractAsyncClientFactory asyncSocketFactory;

	public AbstractAsyncAcceptHandlerFactory getAsyncAcceptHandlerFactory() {
		return acceptHandlerFactory;
	}

	public void setAsyncAcceptHandlerFactory(AbstractAsyncAcceptHandlerFactory acceptHandlerFactory) {
		this.acceptHandlerFactory = acceptHandlerFactory;
	}

	public AbstractAsyncReadHandlerFactory getAsyncReadHandlerFactory() {
		return readHandlerFactory;
	}

	public void setAsyncReadHandlerFactory(AbstractAsyncReadHandlerFactory readHandlerFactory) {
		this.readHandlerFactory = readHandlerFactory;
	}

	public WriteHandlerFactory getAsyncWriteHandlerFactory() {
		return writeHandlerFactory;
	}

	public void setAsyncWriteHandlerFactory(WriteHandlerFactory writeHandlerFactory) {
		this.writeHandlerFactory = writeHandlerFactory;
	}

	@SuppressWarnings("rawtypes")
	public AbstractAsyncClientFactory getAsyncClientFactory() {
		return asyncSocketFactory;
	}

	@SuppressWarnings("rawtypes")
	public void setAsyncClientFactory(AbstractAsyncClientFactory asyncSocketFactory) {
		this.asyncSocketFactory = asyncSocketFactory;
	}

	public AbstractAsyncConnectHandlerFactory getAsyncConnectHandlerFactory() {
		return connectHandlerFactory;
	}

	public void setAsyncConnectHandlerFactory(AbstractAsyncConnectHandlerFactory connectHandlerFactory) {
		this.connectHandlerFactory = connectHandlerFactory;
	}
}