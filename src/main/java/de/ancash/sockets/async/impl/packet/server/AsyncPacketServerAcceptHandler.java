package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;

import de.ancash.sockets.async.server.AbstractAsyncAcceptHandler;
import de.ancash.sockets.async.server.AbstractAsyncServer;

public class AsyncPacketServerAcceptHandler extends AbstractAsyncAcceptHandler{

	public AsyncPacketServerAcceptHandler(AbstractAsyncServer asyncIOServer) {
		super(asyncIOServer);
	}
	
	@Override
	public void failed(Throwable t, AsynchronousServerSocketChannel server) {
		if(t instanceof AsynchronousCloseException) {
			try {
				getAsyncIOServer().stop();
			} catch (IOException e) {}
			return;
		}
		System.err.println("Failed to accept connection: " + t);
		t.printStackTrace();
	}
}