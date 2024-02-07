package de.ancash.sockets.async.forward;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import de.ancash.sockets.async.client.AbstractAsyncClientFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;

public class AsyncForwardConnectionFactory extends AbstractAsyncClientFactory<AsyncForwardConnection> {

	@Override
	public AsyncForwardConnection newInstance(AbstractAsyncServer asyncServer, AsynchronousSocketChannel socket, int readBufSize, int writeBufSize)
			throws IOException {
		return new AsyncForwardConnection((AsyncForwardServer) asyncServer, socket, readBufSize, writeBufSize);
	}

	@Override
	public AsyncForwardConnection newInstance(String address, int port, int readBufSize, int writeBufSize, int threads) throws IOException {
		throw new UnsupportedOperationException();
	}
}