package de.ancash.sockets.async.impl.packet.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executors;

import de.ancash.sockets.async.client.AbstractAsyncClientFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;

public class AsyncPacketClientFactory extends AbstractAsyncClientFactory<AsyncPacketClient>{

	@Override
	public AsyncPacketClient newInstance(AbstractAsyncServer asyncServer, AsynchronousSocketChannel socket, int queueSize, int readBufSize, int writeBufSize) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AsyncPacketClient newInstance(String address, int port, int queueSize, int readBufSize, int writeBufSize, int threads) throws IOException {
		AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(threads));
		AsynchronousSocketChannel asyncSocket = AsynchronousSocketChannel.open(asyncChannelGroup);
		AsyncPacketClient client = new AsyncPacketClient(asyncSocket, asyncChannelGroup, queueSize, readBufSize, writeBufSize, threads);
		asyncSocket.connect(new InetSocketAddress(address, port), client, client.getAsyncConnectHandlerFactory().newInstance(client));
		return client;
	}
}