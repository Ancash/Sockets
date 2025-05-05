package de.ancash.sockets.async.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import de.ancash.sockets.async.FactoryHandler;
import de.ancash.sockets.async.client.AbstractAsyncClientFactory;
import de.ancash.sockets.async.client.DefaultAsyncReadHandler;
import de.ancash.sockets.async.client.DefaultAsyncWriteHandler;
import de.ancash.sockets.async.client.IReadHandlerFactory;
import de.ancash.sockets.async.client.IWriteHandlerFactory;

public abstract class AbstractAsyncServer extends FactoryHandler {

	static final AtomicInteger cnt = new AtomicInteger();

	private final String address;
	private final int port;
	private int readBufSize = 64 * 1024;
	private int writeBufSize = 64 * 1024;
	private AsynchronousServerSocketChannel listener;
	protected IWriteHandlerFactory writeHandlerFactory = s -> new DefaultAsyncWriteHandler(s);
	protected IReadHandlerFactory readHandlerFactory = s -> new DefaultAsyncReadHandler(s, readBufSize, null);

	public AbstractAsyncServer(String address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public void setReadHandlerFactory(IReadHandlerFactory readHandlerFactory) {
		this.readHandlerFactory = readHandlerFactory;
	}
	
	public void setWriteHandlerFactory(IWriteHandlerFactory writeHandlerFactory) {
		this.writeHandlerFactory = writeHandlerFactory;
	}

	public void start() throws IOException {
		listener = AsynchronousServerSocketChannel.open(AbstractAsyncClientFactory.getGroup()).bind(new InetSocketAddress(address, port));
		listener.accept(listener, getAsyncAcceptHandlerFactory().newInstance(this));
	}

	public synchronized void stop() throws IOException {
		if (listener == null)
			return;
		listener.close();
		listener = null;
	}

	public SocketAddress getLocalAddress() throws IOException {
		return listener.getLocalAddress();
	}

	public int getWriteBufSize() {
		return writeBufSize;
	}

	public void setWriteBufSize(int writeBufSize) {
		this.writeBufSize = writeBufSize;
	}

	public int getReadBufSize() {
		return readBufSize;
	}

	public void setReadBufSize(int readBufSize) {
		this.readBufSize = readBufSize;
	}

	public boolean isOpen() {
		return listener != null && listener.isOpen();
	}

	public abstract void onAccept(AsynchronousSocketChannel socket) throws IOException;
}