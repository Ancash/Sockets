package de.ancash.sockets.async.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicLong;

import de.ancash.sockets.async.client.AbstractAsyncClientFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;

public class HttpClientFactory extends AbstractAsyncClientFactory<HttpClient> {

	static final AtomicLong cnt = new AtomicLong();

	@Override
	public HttpClient newInstance(AbstractAsyncServer asyncServer, AsynchronousSocketChannel socket, int readBufSize,
			int writeBufSize) throws IOException {
		return new HttpClient(asyncServer, socket, readBufSize, writeBufSize);
	}

	@Override
	public HttpClient newInstance(String address, int port, int readBufSize, int writeBufSize) throws IOException {
		AsynchronousSocketChannel asyncSocket = AsynchronousSocketChannel.open(getGroup());
		HttpClient client = new HttpClient(asyncSocket, readBufSize, writeBufSize);
		asyncSocket.connect(new InetSocketAddress(address, port), client,
				client.getAsyncConnectHandlerFactory().newInstance(client));
		return client;
	}
}