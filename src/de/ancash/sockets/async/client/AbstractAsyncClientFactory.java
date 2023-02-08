package de.ancash.sockets.async.client;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import de.ancash.sockets.async.server.AbstractAsyncServer;

public abstract class AbstractAsyncClientFactory<T extends AbstractAsyncClient> {

	public abstract T newInstance(AbstractAsyncServer asyncServer, AsynchronousSocketChannel socket, int queueSize, int readBufSize, int writeBufSize) throws IOException;
	
	public abstract T newInstance(String address, int port, int queueSize, int readBufSize, int writeBufSize, int threads) throws IOException;
}