package de.ancash.sockets.async.forward;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ancash.sockets.async.impl.packet.client.AsyncPacketClientReadHandlerFactory;
import de.ancash.sockets.async.impl.packet.client.AsyncPacketClientWriteHandlerFactory;
import de.ancash.sockets.async.impl.packet.server.AsyncPacketServerAcceptHandlerFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;

public class AsyncForwardServer extends AbstractAsyncServer{
	
	private long checkAliveIntervalMs = 5000;
	private final ServerDescription[] serverList;
	private ExecutorService executor;
	
	public AsyncForwardServer(String address, int port, String[] targets) {
		super(address, port);
		setAsyncAcceptHandlerFactory(new AsyncPacketServerAcceptHandlerFactory());
		setAsyncReadHandlerFactory(new AsyncPacketClientReadHandlerFactory());
		setAsyncWriteHandlerFactory(new AsyncPacketClientWriteHandlerFactory());
		setAsyncClientFactory(new AsyncForwardConnectionFactory());
		serverList = new ServerDescription[targets.length];
		for(int i = 0; i<targets.length; i++)
			serverList[i] = new ServerDescription(targets[i].split(":")[0], Integer.valueOf(targets[i].split(":")[1]));
	}

	@Override
	public void start() throws IOException {
		super.start();
		executor = Executors.newCachedThreadPool();
		executor.submit(new ForwardCheckAliveThread(serverList, getCheckAliveIntervalMs()));
		System.out.println("Bound to " + getLocalAddress());
	}
	
	@Override
	public synchronized void stop() throws IOException {
		super.stop();	
		if(executor == null) return;
		executor.shutdownNow();
		executor = null;
	}
	
	public long getCheckAliveIntervalMs() {
		return checkAliveIntervalMs;
	}

	public void setCheckAliveIntervalMs(long checkAliveIntervalMs) {
		this.checkAliveIntervalMs = checkAliveIntervalMs;
	}

	public ServerDescription[] getServersList() {
		return serverList;
	}

	@Override
	public void onAccept(AsynchronousSocketChannel socket) {
		try {
			getAsyncClientFactory().newInstance(this, socket, getWriteQueueSize(), getReadBufSize(), getWriteBufSize());
		} catch(Exception ex) {
			
		}
	}
}