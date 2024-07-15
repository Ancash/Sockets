package de.ancash.sockets.async.impl.packet.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import de.ancash.Sockets;
import de.ancash.datastructures.tuples.Duplet;
import de.ancash.datastructures.tuples.Tuple;
import de.ancash.libs.org.bukkit.event.EventHandler;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.libs.org.bukkit.event.Listener;
import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.AbstractAsyncReadHandler;
import de.ancash.sockets.events.ClientConnectEvent;
import de.ancash.sockets.events.ClientDisconnectEvent;
import de.ancash.sockets.events.ClientPacketReceiveEvent;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCallback;
import de.ancash.sockets.packet.PacketCombiner;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketClient extends AbstractAsyncClient implements Listener {

	private static final ExecutorService workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {

		AtomicInteger cnt = new AtomicInteger();

		@SuppressWarnings("nls")
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "ClientPacketWorker-" + cnt.getAndIncrement());
			return t;
		}
	});
	private static final ArrayBlockingQueue<Duplet<AsyncPacketClient, UnfinishedPacket>> unfinishedPacketsQueue = new ArrayBlockingQueue<>(10_000);

	static {
		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++)
			workerPool.submit(new AsyncPacketClientPacketWorker(unfinishedPacketsQueue, i));
	}

	private final Map<Long, PacketCallback> packetCallbacks = new ConcurrentHashMap<>();
	private final Map<Long, Packet> awaitResponses = new ConcurrentHashMap<>();
	private final AtomicReference<Long> lock = new AtomicReference<>(null);
	private final PacketCombiner packetCombiner;
//	private ExecutorService clientThreadPool = Executors.newCachedThreadPool();

	public AsyncPacketClient(AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize) throws IOException {
		super(asyncSocket, readBufSize, writeBufSize);
		packetCombiner = new PacketCombiner(1024 * 1024 * 8);
		setAsyncClientFactory(new AsyncPacketClientFactory());
		setAsyncReadHandlerFactory(new AsyncPacketClientReadHandlerFactory());
		setAsyncWriteHandlerFactory(new AsyncPacketClientWriteHandlerFactory());
		setAsyncConnectHandlerFactory(new AsyncPacketClientConnectHandlerFactory());
		setHandlers();
	}

	@EventHandler
	public void onPacket(ClientPacketReceiveEvent event) {
		Packet packet = event.getPacket();
		PacketCallback pc;
		Packet awake;
		pc = packetCallbacks.remove(packet.getTimeStamp());
		awake = awaitResponses.remove(packet.getTimeStamp());
		if (pc != null)
			pc.call(packet.getObject());
		if (awake != null)
			awake.awake(packet);
	}

	public final void write(Packet packet) throws InterruptedException {
		try {
			while (!lock.compareAndSet(null, Thread.currentThread().getId())
					&& !lock.compareAndSet(Thread.currentThread().getId(), Thread.currentThread().getId()))
				Sockets.sleepMillis(1);
			packet.addTimeStamp();
			while (packetCallbacks.containsKey(packet.getTimeStamp()) || awaitResponses.containsKey(packet.getTimeStamp()))
				packet.addTimeStamp();
			if (packet.hasPacketCallback()) {
				packetCallbacks.put(packet.getTimeStamp(), packet.getPacketCallback());
			}
			if (packet.isAwaitingRespose())
				awaitResponses.put(packet.getTimeStamp(), packet);
		} finally {
			lock.set(null);
		}

		putWrite(packet.toBytes());
	}

	@Override
	public void onBytesReceive(ByteBuffer b) {
		for (UnfinishedPacket packet : packetCombiner.put(b))
			try {
				unfinishedPacketsQueue.put(Tuple.of(this, packet));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void onConnect() {
		EventManager.registerEvents(this, this);
		EventManager.callEvent(new ClientConnectEvent(this));
	}

	@Override
	public synchronized void onDisconnect(Throwable th) {
		try {
			getAsyncSocketChannel().close();
		} catch (IOException e) {

		}
		EventManager.callEvent(new ClientDisconnectEvent(this, th));
		try {
			while (!lock.compareAndSet(null, Thread.currentThread().getId())
					&& !lock.compareAndSet(Thread.currentThread().getId(), Thread.currentThread().getId()))
				Sockets.sleepMillis(1);
			for (PacketCallback callback : packetCallbacks.values()) {
				try {
					callback.call(th);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (Packet packet : awaitResponses.values()) {
				try {
					packet.awake(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			packetCallbacks.clear();
			awaitResponses.clear();
		} finally {
			lock.set(null);
		}
	}

	@Override
	public boolean isConnectionValid() {
		return isConnected();
	}

	@Override
	public boolean delayNextRead() {
		return false;
	}
}