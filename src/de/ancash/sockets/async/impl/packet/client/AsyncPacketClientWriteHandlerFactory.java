package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncWriteHandler;
import de.ancash.sockets.async.client.AbstractAsyncWriteHandlerFactory;

public class AsyncPacketClientWriteHandlerFactory extends AbstractAsyncWriteHandlerFactory {

	@Override
	public AbstractAsyncWriteHandler newInstance(AbstractAsyncClient asyncClient) {
		return new AsyncPacketClientWriteHandler(asyncClient);
	}
}