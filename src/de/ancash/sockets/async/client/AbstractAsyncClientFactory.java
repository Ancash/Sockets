package de.ancash.sockets.async.client;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;

import de.ancash.sockets.async.server.AbstractAsyncServer;

public abstract class AbstractAsyncClientFactory<T extends AbstractAsyncClient> {

	protected static AsynchronousChannelGroup asyncChannelGroup = AbstractAsyncClient.asyncChannelGroup;

	public abstract T newInstance(AbstractAsyncServer asyncServer, AsynchronousSocketChannel socket, int readBufSize,
			int writeBufSize) throws IOException;

	public abstract T newInstance(String address, int port, int readBufSize, int writeBufSize, int threads)
			throws IOException;
}