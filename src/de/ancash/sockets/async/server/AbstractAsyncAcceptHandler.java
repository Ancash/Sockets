package de.ancash.sockets.async.server;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public abstract class AbstractAsyncAcceptHandler
		implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

	private final AbstractAsyncServer asyncIOServer;

	public AbstractAsyncAcceptHandler(AbstractAsyncServer asyncIOServer) {
		this.asyncIOServer = asyncIOServer;
	}

	public AbstractAsyncServer getAsyncIOServer() {
		return asyncIOServer;
	}

	@Override
	public void completed(AsynchronousSocketChannel socket, AsynchronousServerSocketChannel server) {
		if (!asyncIOServer.isOpen()) {
			failed(new AsynchronousCloseException(), server);
			return;
		}

		try {
			socket.setOption(StandardSocketOptions.SO_SNDBUF, asyncIOServer.getWriteBufSize());
			socket.setOption(StandardSocketOptions.SO_RCVBUF, asyncIOServer.getReadBufSize());
			asyncIOServer.onAccept(socket);
		} catch (IOException ex) {
			failed(ex, server);
		}
		server.accept(server, this);
	}
}