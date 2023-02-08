package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncReadHandler;

public class AsyncPacketClientReadHandler extends AbstractAsyncReadHandler{

	public AsyncPacketClientReadHandler(AbstractAsyncClient asyncClient, int readBufSize) {
		super(asyncClient, readBufSize);
	}
}