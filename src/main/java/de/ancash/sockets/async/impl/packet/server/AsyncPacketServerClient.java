package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.server.AbstractAsyncServer;
import de.ancash.sockets.packet.PacketCombiner;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketServerClient extends AbstractAsyncClient{
	
	private final AsyncPacketServer server;
	private final PacketCombiner packetCombiner = new PacketCombiner();
	
	public AsyncPacketServerClient(AbstractAsyncServer asyncIOServer, AsynchronousSocketChannel asyncSocket, int queueSize, int readBufSize, int writeBufSize) throws IOException  {
		super(asyncSocket, queueSize, readBufSize, writeBufSize);
		this.server = (AsyncPacketServer) asyncIOServer;
		setAsyncWriteHandlerFactory(asyncIOServer.getAsyncWriteHandlerFactory());
		setAsyncReadHandlerFactory(asyncIOServer.getAsyncReadHandlerFactory());
		setConnected(true);
		setHandlers();
		startReadHandler();
	}

	@Override
	public void onConnect() {}

	@Override
	public void onBytesReceive(byte[] bytes) {
		for(UnfinishedPacket unfinished : packetCombiner.put(bytes))
			server.onPacket(unfinished, this);
	}

	@Override
	public boolean isConnectionValid() {
		return isConnected();
	}

	@Override
	public void onDisconnect(Throwable th) {
		server.onDisconnect(this, th);
	}
}