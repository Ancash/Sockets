package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncConnectHandler;

public class AsyncPacketClientConnectHandler extends AbstractAsyncConnectHandler{

	public AsyncPacketClientConnectHandler(AbstractAsyncClient client) {
		super(client);
	}
	
	@Override
	public void completed(Void arg0, AbstractAsyncClient arg1) {
		super.completed(arg0, arg1);
	}
	
	@Override
	public void failed(Throwable arg0, AbstractAsyncClient arg1) {
		super.failed(arg0, arg1);
	}
}