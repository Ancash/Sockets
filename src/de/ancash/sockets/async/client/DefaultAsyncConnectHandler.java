package de.ancash.sockets.async.client;

import java.nio.channels.CompletionHandler;

public class DefaultAsyncConnectHandler implements CompletionHandler<Void, AbstractAsyncClient> {

	protected final AbstractAsyncClient client;

	public DefaultAsyncConnectHandler(AbstractAsyncClient client) {
		this.client = client;
	}

	@Override
	public void completed(Void arg0, AbstractAsyncClient arg1) {
		client.setConnected(true);
		client.startReadHandler();
		client.onConnect();
	}

	@Override
	public void failed(Throwable arg0, AbstractAsyncClient arg1) {
		client.onDisconnect(arg0);
	}
}