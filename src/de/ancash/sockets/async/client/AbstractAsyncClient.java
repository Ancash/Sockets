package de.ancash.sockets.async.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import de.ancash.Sockets;
import de.ancash.sockets.async.FactoryHandler;
import de.ancash.sockets.io.FixedByteBuffer;
import de.ancash.sockets.io.PositionedByteBuf;

public abstract class AbstractAsyncClient extends FactoryHandler {

	public static AsynchronousChannelGroup asyncChannelGroup;

	static {
		try {
			asyncChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(
					Math.max(Runtime.getRuntime().availableProcessors() / 2, 1), new ThreadFactory() {
						int i = 0;

						@Override
						public synchronized Thread newThread(Runnable arg0) {
							Thread t = new Thread(arg0);
							t.setName("AsyncChannelGroup-" + i++);
							return t;
						}
					});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final AtomicInteger clid = new AtomicInteger();

	protected final int instance = clid.getAndIncrement();
	protected final int readBufSize;
	protected final int writeBufSize;
	protected final AsynchronousSocketChannel asyncSocket;
	protected AbstractAsyncReadHandler readHandler;
	protected AbstractAsyncWriteHandler writeHandler;
	protected SocketAddress remoteAddress;
	protected SocketAddress localAddress;
	protected final AtomicBoolean isConnected = new AtomicBoolean(false);
	protected TimeUnit timeoutunit = TimeUnit.SECONDS;
	protected long timeout = Long.MAX_VALUE;
	protected final ReentrantLock lock = new ReentrantLock(true);
	protected AtomicLong pos = new AtomicLong();
	protected long stamp = System.currentTimeMillis();
	protected final FixedByteBuffer fbb;
	public final AtomicBoolean reading = new AtomicBoolean();
	public final AtomicBoolean writing = new AtomicBoolean();

	@SuppressWarnings("nls")
	public AbstractAsyncClient(AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize)
			throws IOException {
		if (asyncSocket == null || !asyncSocket.isOpen())
			throw new IllegalArgumentException("Invalid AsynchronousSocketChannel");
		this.readBufSize = readBufSize;
		this.writeBufSize = writeBufSize;
		this.asyncSocket = asyncSocket;
		this.fbb = new FixedByteBuffer(writeBufSize, 64, 1);
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

	@SuppressWarnings("nls")
	public void startReadHandler(long t, TimeUnit tu) {
		this.timeout = t;
		this.timeoutunit = tu;
		try {
			this.remoteAddress = asyncSocket.getRemoteAddress();
			this.localAddress = asyncSocket.getLocalAddress();
		} catch (IOException ex) {
			System.err.println("could not get local/remote socket address");
		}

		reading.set(true);
		ByteBuffer buf = ByteBuffer.allocate(readBufSize);
		asyncSocket.read(buf, timeout, timeoutunit, buf, readHandler);
	}

	public FixedByteBuffer getBuffer() {
		return fbb;
	}

	public void putWrite(byte[] b) throws InterruptedException {
		putWrite(ByteBuffer.wrap(b));
	}

	public void putWrite(ByteBuffer bb) throws InterruptedException {
		fbb.put(bb);
//		bufs.put(bb);
		checkWrite();
	}

	LinkedBlockingQueue<ByteBuffer> bufs = new LinkedBlockingQueue<>(1000);

	public void checkWrite() throws InterruptedException {
		lock.lock();
		try {
			if (!fbb.canRead())
				return;
//			if(bufs.isEmpty())
//				return;
			if (writeHandler.canWrite()) {
				PositionedByteBuf pbb = fbb.read();
				writeHandler.write(pbb);
//				writeHandler.write(bufs.take());
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

	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public void setConnected(boolean b) {
		this.isConnected.set(b);
		if (!b)
			readHandler.onDisconnect();
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