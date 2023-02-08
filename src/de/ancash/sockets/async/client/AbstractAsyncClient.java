package de.ancash.sockets.async.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import de.ancash.sockets.async.FactoryHandler;

public abstract class AbstractAsyncClient extends FactoryHandler{

	protected final int readBufSize;
	protected final int writeBufSize;
	protected final AsynchronousSocketChannel asyncSocket;
	protected AbstractAsyncReadHandler readHandler;
	protected AbstractAsyncWriteHandler writeHandler;
	protected final LinkedBlockingQueue<ByteBuffer> toWrite;
	protected final SocketAddress remoteAddress;
	protected final AtomicBoolean isConnected = new AtomicBoolean(false);
	protected TimeUnit timeoutunit = TimeUnit.SECONDS;
	protected long timeout = Long.MAX_VALUE;
	protected final ReentrantLock lock = new ReentrantLock(true);
	
	@SuppressWarnings("nls")
	public AbstractAsyncClient(AsynchronousSocketChannel asyncSocket, int queueSize, int readBufSize, int writeBufSize) throws IOException {
		if(asyncSocket == null || !asyncSocket.isOpen())
			throw new IllegalArgumentException("Invalid AsynchronousSocketChannel");
		this.readBufSize = readBufSize;
		this.writeBufSize = writeBufSize;
		this.toWrite = new LinkedBlockingQueue<>(queueSize);
		this.asyncSocket = asyncSocket;
		this.remoteAddress = asyncSocket.getRemoteAddress();
		asyncSocket.setOption(StandardSocketOptions.SO_RCVBUF, readBufSize);
		asyncSocket.setOption(StandardSocketOptions.SO_SNDBUF, writeBufSize);
	}
	
	public void setHandlers() {
		this.readHandler = getAsyncReadHandlerFactory().newInstance(this, readBufSize);
		this.writeHandler = getAsyncWriteHandlerFactory().newInstance(this);
	}
	
	public void startReadHandler() {
		startReadHandler(timeout, timeoutunit);
	}
	
	public void startReadHandler(long t, TimeUnit tu) {
		this.timeout = t;
		this.timeoutunit = tu;
		asyncSocket.read(readHandler.readBuffer, timeout, timeoutunit, readHandler.readBuffer, readHandler);
	}
	
	public boolean offerWrite(byte[] b) {
		return offerWrite(ByteBuffer.wrap(b));
	}
	
	
	public boolean offerWrite(ByteBuffer bb) {
		if(toWrite.offer(bb)) {
			checkWrite();
			return true;
		}
		return false;
	}
	
	public boolean putWrite(byte[] b) {
		return putWrite(ByteBuffer.wrap(b));
	}
	
	public boolean putWrite(ByteBuffer bb) {
		return putWrite(bb, 1, TimeUnit.MINUTES);
	}
	
	public boolean putWrite(ByteBuffer bb, long timeout, TimeUnit unit) {
		try {
			toWrite.offer(bb, timeout, unit);
			checkWrite();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	public int getWritingQueueSize() {
		return toWrite.size();
	}

	public void checkWrite() {
		lock.lock();
		try {
			if(toWrite.isEmpty()) return;
			if(writeHandler.canWrite()) {
				ByteBuffer bb = toWrite.poll();
				writeHandler.write(bb);
			}
		} finally {
			lock.unlock();
		}
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

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}
	
	public void setConnected(boolean b) {
		this.isConnected.set(b);
	}
	
	public void setTimeout(long l, TimeUnit u) {
		this.timeout = l;
		this.timeoutunit = u;
	}
	
	public abstract boolean isConnectionValid();
	
	public abstract void onBytesReceive(byte[] bytes);
	
	public abstract void onConnect();
	
	public abstract void onDisconnect(Throwable th);
}