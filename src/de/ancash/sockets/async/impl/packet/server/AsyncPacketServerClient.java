package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.server.AbstractAsyncServer;
import de.ancash.sockets.io.DistributedByteBuffer;
import de.ancash.sockets.packet.PacketCombiner;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketServerClient extends AbstractAsyncClient {

	protected final AsyncPacketServer server;
	protected final PacketCombiner packetCombiner;

	public AsyncPacketServerClient(AbstractAsyncServer asyncIOServer, AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize)
			throws IOException {
		super(asyncSocket, readBufSize, writeBufSize, false);
		packetCombiner = new PacketCombiner(1024 * 1024, 16);
		this.server = (AsyncPacketServer) asyncIOServer;
		setAsyncWriteHandlerFactory(asyncIOServer.getAsyncWriteHandlerFactory());
		setAsyncReadHandlerFactory(asyncIOServer.getAsyncReadHandlerFactory());
		setConnected(true);
		setHandlers();
	}

	public int getMaxPacketSize() {
		return packetCombiner.getMaxSize();
	}
	
	public void freeReadBuffer(DistributedByteBuffer dbb) {
		packetCombiner.freeBuffer(dbb);
	}

	@Override
	public void onConnect() {
	}

	@Override
	public void onBytesReceive(ByteBuffer bytes) {
		List<UnfinishedPacket> l;
		try {
			l = packetCombiner.put(bytes);
		} catch (Exception ex) {
			try {
				System.err.println(getRemoteAddress() + " threw an exception during read: " + ex);
				ex.printStackTrace();
				System.err.println("Disconnecting " + getRemoteAddress());
				getAsyncSocketChannel().close();
			} catch (IOException e) {

			}
			return;
		} finally {

		}
		for (UnfinishedPacket unfinished : l)
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