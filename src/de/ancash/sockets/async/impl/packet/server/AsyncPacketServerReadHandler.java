package de.ancash.sockets.async.impl.packet.server;

import com.lmax.disruptor.EventHandler;

import de.ancash.sockets.async.ByteEvent;
import de.ancash.sockets.async.ByteEventHandler;
import de.ancash.sockets.async.client.AbstractAsyncReadHandler;

public class AsyncPacketServerReadHandler extends AbstractAsyncReadHandler
		implements ByteEventHandler, EventHandler<ByteEvent> {

	public AsyncPacketServerReadHandler(AsyncPacketServer server, AsyncPacketServerClient asyncClient,
			int readBufSize) {
		super(asyncClient, readBufSize, null);
		super.byteHandler = this;
	}

	@Override
	public void onBytes(byte[] arr) {
		client.onBytesReceive(arr);
	}

	@Override
	public void onEvent(ByteEvent event, long sequence, boolean endOfBatch) throws Exception {
		client.onBytesReceive(event.bytes);
	}

	@Override
	public void onDisconnect() {

	}
}