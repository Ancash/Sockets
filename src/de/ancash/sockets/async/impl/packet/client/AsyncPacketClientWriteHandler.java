package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.GatheringWriteHandler;

public class AsyncPacketClientWriteHandler extends GatheringWriteHandler {

	public AsyncPacketClientWriteHandler(AbstractAsyncClient asyncClient) {
		super(asyncClient);
	}
}