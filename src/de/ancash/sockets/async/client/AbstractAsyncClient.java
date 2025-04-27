package de.ancash.sockets.async.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import de.ancash.sockets.async.FactoryHandler;

public abstract class AbstractAsyncClient extends FactoryHandler {

	private static final AtomicInteger clid = new AtomicInteger();

	protected final int instance = clid.getAndIncrement();
	protected final int readBufSize;
	protected final int writeBufSize;
	protected final AsynchronousSocketChannel asyncSocket;
	protected AbstractAsyncReadHandler readHandler;
	protected IWriteHandler writeHandler;
	protected SocketAddress remoteAddress;
	protected SocketAddress localAddress;
	protected final AtomicBoolean isConnected = new AtomicBoolean(false);
	protected TimeUnit timeoutunit = TimeUnit.SECONDS;
	protected long timeout = Long.MAX_VALUE;
	protected final ReentrantLock lock = new ReentrantLock(true);
	protected AtomicLong pos = new AtomicLong();
	protected long stamp = System.currentTimeMillis();

	public AbstractAsyncClient(AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize, boolean isClientSide) throws IOException {
		if (asyncSocket == null || !asyncSocket.isOpen())
			throw new IllegalArgumentException("Invalid AsynchronousSocketChannel");
		this.readBufSize = readBufSize;
		this.writeBufSize = writeBufSize;
		this.asyncSocket = asyncSocket;
		asyncSocket.setOption(StandardSocketOptions.SO_RCVBUF, readBufSize);
		asyncSocket.setOption(StandardSocketOptions.SO_SNDBUF, writeBufSize);
//		asyncSocket.setOption(StandardSocketOptions.TCP_NODELAY, true);
	}

	public void setHandlers() {
		this.readHandler = getAsyncReadHandlerFactory().newInstance(this, readBufSize);
		this.writeHandler = getAsyncWriteHandlerFactory().newInstance(this);
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 == null)
			return false;
		if (!(arg0 instanceof AbstractAsyncClient))
			return false;
		return instance == ((AbstractAsyncClient) arg0).instance;
	}

	@Override
	public int hashCode() {
		return instance;
	}

	public void startReadHandler() {
		startReadHandler(timeout, timeoutunit);
	}

	public void startReadHandler(long t, TimeUnit tu) {
		this.timeout = t;
		this.timeoutunit = tu;
		try {
			this.remoteAddress = asyncSocket.getRemoteAddress();
			this.localAddress = asyncSocket.getLocalAddress();
		} catch (IOException ex) {
			System.err.println("could not get local/remote socket address");
		}
		readHandler.tryInitRead();
	}

	public void putWrite(byte[] b) throws InterruptedException {
		putWrite(ByteBuffer.wrap(b));
	}

	public void putWrite(ByteBuffer bb) {
		writeHandler.write(bb);
	}

	public boolean isConnected() {
		return isConnected.get() && asyncSocket.isOpen();
	}

	public int getWriteBufSize() {
		return writeBufSize;
	}

	public int getReadBufSize() {
		return readBufSize;
	}

	public AsynchronousSocketChannel getAsyncSocketChannel() {
		return asyncSocket;
	}

	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public void setConnected(boolean b) {
		this.isConnected.set(b);
		if (!b) {
			readHandler.onDisconnect();
		}
	}

	public void setTimeout(long l, TimeUnit u) {
		this.timeout = l;
		this.timeoutunit = u;
	}

	public abstract boolean isConnectionValid();

	public abstract void onBytesReceive(ByteBuffer bytes);

	public abstract void onConnect();

	public abstract void onDisconnect(Throwable th);
}