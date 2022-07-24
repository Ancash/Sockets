package de.ancash.sockets.async.client;

import java.nio.channels.CompletionHandler;

public abstract class AbstractAsyncConnectHandler implements CompletionHandler<Void, AbstractAsyncClient>{

	protected final AbstractAsyncClient client;
	
	public AbstractAsyncConnectHandler(AbstractAsyncClient client) {
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