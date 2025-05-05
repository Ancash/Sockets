package de.ancash.sockets.async;

import de.ancash.sockets.async.client.AbstractAsyncClientFactory;
import de.ancash.sockets.async.client.AbstractAsyncConnectHandlerFactory;
import de.ancash.sockets.async.server.IAcceptHandlerFactory;

public class FactoryHandler {

	private IAcceptHandlerFactory acceptHandlerFactory;
	private AbstractAsyncConnectHandlerFactory connectHandlerFactory;
	@SuppressWarnings("rawtypes")
	private AbstractAsyncClientFactory asyncSocketFactory;

	public IAcceptHandlerFactory getAsyncAcceptHandlerFactory() {
		return acceptHandlerFactory;
	}

	public void setAsyncAcceptHandlerFactory(IAcceptHandlerFactory acceptHandlerFactory) {
		this.acceptHandlerFactory = acceptHandlerFactory;
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