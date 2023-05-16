package de.ancash.sockets.async.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executors;

import de.ancash.sockets.async.FactoryHandler;

public abstract class AbstractAsyncServer extends FactoryHandler {

	private final String address;
	private final int port;
	private int threads = Runtime.getRuntime().availableProcessors() * 2;
	private int readBufSize = 8 * 1024;
	private int writeBufSize = 8 * 1024;
	private int writeQueueSize = 1000;
	private AsynchronousChannelGroup asyncChannelGroup;
	private AsynchronousServerSocketChannel listener;

	public AbstractAsyncServer(String address, int port) {
		this.address = address;
		this.port = port;
	}

	public void start() throws IOException {
		stop();
		asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(threads));
		listener = AsynchronousServerSocketChannel.open(asyncChannelGroup).bind(new InetSocketAddress(address, port));
		listener.accept(listener, getAsyncAcceptHandlerFactory().newInstance(this));
	}

	public synchronized void stop() throws IOException {
		if (asyncChannelGroup == null)
			return;
		asyncChannelGroup.shutdownNow();
		listener.close();
		asyncChannelGroup = null;
		listener = null;
	}

	public SocketAddress getLocalAddress() throws IOException {
		return listener.getLocalAddress();
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int i) {
		this.threads = i;
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

	public int getWriteQueueSize() {
		return writeQueueSize;
	}

	public void setWriteQueueSize(int writeQueueSize) {
		this.writeQueueSize = writeQueueSize;
	}

	public boolean isOpen() {
		return listener != null && listener.isOpen();
	}

	public abstract void onAccept(AsynchronousSocketChannel socket) throws IOException;
}