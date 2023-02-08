package de.ancash.sockets.async.impl.packet.client;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncReadHandlerFactory;

public class AsyncPacketClientReadHandlerFactory extends AbstractAsyncReadHandlerFactory{

	@Override
	public AsyncPacketClientReadHandler newInstance(AbstractAsyncClient socket, int readBufSize) {
		return new AsyncPacketClientReadHandler(socket, readBufSize);
	}
}