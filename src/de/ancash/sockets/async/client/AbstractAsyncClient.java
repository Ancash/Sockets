package de.ancash.sockets.async.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import de.ancash.sockets.async.FactoryHandler;

public abstract class AbstractAsyncClient extends FactoryHandler {

	private static final AtomicLong clid = new AtomicLong();

	protected final long instance = clid.incrementAndGet();
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
	protected AtomicLong pos = new AtomicLong();
	protected final long[] sent = new long[] { 1, 1 };
	protected long stamp = System.currentTimeMillis();
	protected long rateLimit = 10_000;

	@SuppressWarnings("nls")
	public AbstractAsyncClient(AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize)
			throws IOException {
		if (asyncSocket == null || !asyncSocket.isOpen())
			throw new IllegalArgumentException("Invalid AsynchronousSocketChannel");
		this.readBufSize = readBufSize;
		this.writeBufSize = writeBufSize;
		this.toWrite = new LinkedBlockingQueue<>(100);
		this.asyncSocket = asyncSocket;
		this.remoteAddress = asyncSocket.getRemoteAddress();
		asyncSocket.setOption(StandardSocketOptions.SO_RCVBUF, readBufSize);
		asyncSocket.setOption(StandardSocketOptions.SO_SNDBUF, writeBufSize);
		asyncSocket.setOption(StandardSocketOptions.TCP_NODELAY, true);
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
		return hashCode() == ((AbstractAsyncClient) arg0).hashCode();
	}

	@Override
	public int hashCode() {
		if (remoteAddress == null)
			return 0;
		return getRemoteAddress().hashCode();
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
		ByteBuffer bb = ByteBuffer.allocateDirect(b.length);
		bb.put(b);
		bb.position(0);
		return offerWrite(bb);
	}

	public boolean offerWrite(ByteBuffer bb) {
		if (toWrite.offer(bb)) {
			checkWrite();
			return true;
		}
		return false;
	}

	public boolean putWrite(byte[] b) {
		ByteBuffer bb = ByteBuffer.allocateDirect(b.length);
		bb.put(b);
		bb.position(0);
		return putWrite(bb);
	}

	public boolean putWrite(ByteBuffer bb) {
		return putWrite(bb, 1, TimeUnit.MINUTES);
	}

	public boolean putWrite(ByteBuffer bb, long timeout, TimeUnit unit) {
		List<ByteBuffer> list = new ArrayList<>();
		if (bb.remaining() > writeBufSize) {
			while (bb.hasRemaining()) {
				byte[] arr = new byte[Math.min(bb.remaining(), writeBufSize)];
				bb.get(arr);
				list.add(ByteBuffer.wrap(arr));
			}
		} else {
			list.add(bb);
		}
		synchronized (this) {

//			ByteBuffer last = list.get(list.size() - 1);
//			if(writeBufSize - last.remaining() > 42 + space) {
//				Packet fill = new Packet(Packet.FILL);
//				fill.setSerializable(new byte[writeBufSize - last.remaining() - 42 - space]);
//				ByteBuffer filled = ByteBuffer.allocate(writeBufSize - space);
//				filled.put(last.array());
//				filled.put(fill.toBytes().array());
//				list.set(list.size() - 1, filled);
//			}
			try {
				for (int i = 0; i < list.size(); i++) {
					int size = list.get(i).remaining();
					toWrite.offer(list.get(i), timeout, unit);
					incrementeSent(size);
					if (writeHandler.canWrite() && !(i < list.size() - 1 && toWrite.remainingCapacity() > 0))
						checkWrite();
				}
			} catch (InterruptedException e) {
				System.err.println("interrupted write: " + e);
				return false;
			}
		}
		checkWrite();
		return true;
	}

	protected void incrementeSent(int size) {
		if (stamp + TimeUnit.SECONDS.toMillis(10) < System.currentTimeMillis()) {
			stamp = System.currentTimeMillis();
			sent[(int) (pos.incrementAndGet() % sent.length)] = size;
			rateLimit = AbstractAsyncWriteHandler
					.calcRateLimit((int) (sent[(int) ((pos.get() + 1) % sent.length)] / 10));
//			System.out.println("calc rate limit: " + rateLimit + " ns, bytesps: " + sent[(int) ((pos.get() + 1) % sent.length)] / 10);
		} else {
			sent[(int) (pos.get() % sent.length)] += size;
			;
		}

	}

	public int getWritingQueueSize() {
		return toWrite.size();
	}

	public void checkWrite() {
		lock.lock();
		try {
			if (toWrite.isEmpty())
				return;
			if (writeHandler.canWrite()) {
				ByteBuffer bb = toWrite.poll();
				ByteBuffer peek = null;
				while ((peek = toWrite.peek()) != null && bb.remaining() + peek.remaining() <= writeBufSize) {
					bb = ByteBuffer.allocateDirect(bb.remaining() + peek.remaining()).put(bb).put(toWrite.poll())
							.position(0);
				}
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