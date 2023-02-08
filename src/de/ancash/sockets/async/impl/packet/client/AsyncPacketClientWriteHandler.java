package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncWriteHandler;

public class AsyncPacketClientWriteHandler extends AbstractAsyncWriteHandler{

	public AsyncPacketClientWriteHandler(AbstractAsyncClient asyncClient) {
		super(asyncClient);
	}
}