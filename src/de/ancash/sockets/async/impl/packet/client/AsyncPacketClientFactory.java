package de.ancash.sockets.async.impl.packet.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;

import de.ancash.ithread.IThreadPoolExecutor;
import de.ancash.sockets.async.client.AbstractAsyncClientFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;

public class AsyncPacketClientFactory extends AbstractAsyncClientFactory<AsyncPacketClient> {

	@Override
	public AsyncPacketClient newInstance(AbstractAsyncServer asyncServer, AsynchronousSocketChannel socket,
			int readBufSize, int writeBufSize) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AsyncPacketClient newInstance(String address, int port, int readBufSize, int writeBufSize, int threads)
			throws IOException {
		AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup
				.withThreadPool(IThreadPoolExecutor.newFixedThreadPool(threads));
		AsynchronousSocketChannel asyncSocket = AsynchronousSocketChannel.open(asyncChannelGroup);
		AsyncPacketClient client = new AsyncPacketClient(asyncSocket, asyncChannelGroup, readBufSize, writeBufSize,
				threads);
		asyncSocket.connect(new InetSocketAddress(address, port), client,
				client.getAsyncConnectHandlerFactory().newInstance(client));
		return client;
	}
}