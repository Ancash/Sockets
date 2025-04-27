package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.IWriteHandler;
import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.WriteHandlerFactory;

public class AsyncPacketClientWriteHandlerFactory extends WriteHandlerFactory {

	@Override
	public IWriteHandler newInstance(AbstractAsyncClient asyncClient) {
		return new AsyncPacketClientWriteHandler(asyncClient);
	}
}