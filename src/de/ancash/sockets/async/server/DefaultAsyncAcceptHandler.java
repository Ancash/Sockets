package de.ancash.sockets.async.server;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class DefaultAsyncAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

	private final AbstractAsyncServer asyncIOServer;

	public DefaultAsyncAcceptHandler(AbstractAsyncServer asyncIOServer) {
		this.asyncIOServer = asyncIOServer;
	}

	public AbstractAsyncServer getServer() {
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
//			socket.setOption(StandardSocketOptions.TCP_NODELAY, true);
			asyncIOServer.onAccept(socket);
		} catch (IOException ex) {
			failed(ex, server);
		}
		server.accept(server, this);
	}

	@Override
	public void failed(Throwable arg0, AsynchronousServerSocketChannel arg1) {
		if (arg0 instanceof AsynchronousCloseException) {
			try {
				getServer().stop();
			} catch (IOException e) {
			}
			return;
		}
		System.err.println("Failed to accept connection: " + arg0);
		arg0.printStackTrace();
	}
}