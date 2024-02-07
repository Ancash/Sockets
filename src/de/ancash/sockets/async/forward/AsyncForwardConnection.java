package de.ancash.sockets.async.forward;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.ancash.sockets.async.client.AbstractAsyncClient;

public class AsyncForwardConnection extends AbstractAsyncClient {

	private final AsyncForwardServer forwardServer;
	private final AsyncForwardConnection partner;
	private final ServerDescription server;
	private boolean connected = true;

	public AsyncForwardConnection(AsyncForwardServer server, AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize)
			throws IOException {
		super(asyncSocket, readBufSize, writeBufSize);
		this.forwardServer = server;
		this.server = getServerWithMinimalLoad(forwardServer);
		this.partner = new AsyncForwardConnection(this.server, server, createAsyncSocket(this), readBufSize, writeBufSize, this);
		setConnected(true);
		System.out.println(getRemoteAddress() + " <--> " + asyncSocket.getLocalAddress() + " <--> " + partner.getRemoteAddress());
		setAsyncReadHandlerFactory(server.getAsyncReadHandlerFactory());
		setAsyncWriteHandlerFactory(server.getAsyncWriteHandlerFactory());
		setHandlers();
		startReadHandler();
	}

	AsyncForwardConnection(ServerDescription desc, AsyncForwardServer server, AsynchronousSocketChannel asyncSocket, int readBufSize,
			int writeBufSize, AsyncForwardConnection partner) throws IOException {
		super(asyncSocket, readBufSize, writeBufSize);
		setConnected(true);
		this.forwardServer = server;
		this.partner = partner;
		this.server = desc;
		setAsyncReadHandlerFactory(server.getAsyncReadHandlerFactory());
		setAsyncWriteHandlerFactory(server.getAsyncWriteHandlerFactory());
		setHandlers();
		startReadHandler();
	}

	public void writeToPartner(byte[] bytes) {
		partner.putWrite(bytes);
	}

	public void writeToPartner(ByteBuffer bb) {
		partner.putWrite(bb);
	}

	private static synchronized AsynchronousSocketChannel createAsyncSocket(AsyncForwardConnection c) throws IOException {
		if (c.server == null)
			throw new IOException("All servers are dead!");
		AsynchronousSocketChannel asyncSocket = AsynchronousSocketChannel.open();
		try {
			asyncSocket.connect(new InetSocketAddress(c.server.host, c.server.port)).get(3, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			c.server.dead();
			System.err.println("Connection refused: " + c.server.host + ":" + c.server.port);
			throw new IOException(e);
		}
		c.server.clientConnected(c);
		return asyncSocket;
	}

	private static ServerDescription getServerWithMinimalLoad(AsyncForwardServer server) {
		ServerDescription minLoadServer = null;
		ArrayList<ServerDescription> shuffled = new ArrayList<>();
		for (ServerDescription desc : server.getServersList())
			shuffled.add(desc);
		Collections.shuffle(shuffled);
		for (int i = 0; i < shuffled.size(); i++)
			if (shuffled.get(i).isAlive())
				if ((minLoadServer == null) || (shuffled.get(i).countConnected() < minLoadServer.countConnected()))
					minLoadServer = shuffled.get(i);

		return minLoadServer;
	}

	@Override
	public void onDisconnect(Throwable th) {
		synchronized (this) {
			if (!connected)
				return;
			connected = false;
		}
		System.out.println(getRemoteAddress() + " <--> " + partner.getRemoteAddress() + " disconnected!");
		try {
			getAsyncSocketChannel().close();
		} catch (IOException e) {
		}
		server.clientDisconnected(this);
		partner.partnerDisconnect(th);
	}

	private synchronized void partnerDisconnect(Throwable th) {
		synchronized (this) {
			if (!connected)
				return;
			connected = false;
		}
		setConnected(false);
		try {
			getAsyncSocketChannel().close();
		} catch (IOException e) {
		}
		server.clientDisconnected(this);
	}

	@Override
	public void onConnect() {

	}

	@Override
	public void onBytesReceive(byte[] bytes) {
		writeToPartner(bytes);
	}

	@Override
	public boolean isConnectionValid() {
		return isConnected() && partner.isConnected();
	}
}