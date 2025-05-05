package de.ancash.sockets.async.impl.packet.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.ancash.Sockets;
import de.ancash.datastructures.tuples.Duplet;
import de.ancash.datastructures.tuples.Tuple;
import de.ancash.libs.org.bukkit.event.EventHandler;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.libs.org.bukkit.event.Listener;
import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.DefaultAsyncConnectHandler;
import de.ancash.sockets.events.ClientConnectEvent;
import de.ancash.sockets.events.ClientDisconnectEvent;
import de.ancash.sockets.events.ClientPacketReceiveEvent;
import de.ancash.sockets.io.DistributedByteBuffer;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCallback;
import de.ancash.sockets.packet.PacketCombiner;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketClient extends AbstractAsyncClient implements Listener {

	private static final Set<Long> workers = new HashSet<Long>();
	private static final ExecutorService workerPool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 8, 1),
			new ThreadFactory() {

				AtomicInteger cnt = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					
					Thread t = new Thread(r, "ClientPacketWorker-" + cnt.getAndIncrement());
					workers.add(t.getId());
					return t;
				}
			});

	private static final ExecutorService exec = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 8, 1),
			new ThreadFactory() {
				AtomicInteger cnt = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "PacketWriter-" + cnt.getAndIncrement());
				}
			});

	private static final ArrayBlockingQueue<Duplet<AsyncPacketClient, UnfinishedPacket>> unfinishedPacketsQueue = new ArrayBlockingQueue<>(1_000);

	static {
		for (int i = 0; i < Math.max(Runtime.getRuntime().availableProcessors() / 8, 1); i++)
			workerPool.submit(new AsyncPacketClientPacketWorker(unfinishedPacketsQueue, i));
	}

	private final Map<Long, PacketCallback> packetCallbacks = new ConcurrentHashMap<>();
	private final Map<Long, Packet> awaitResponses = new ConcurrentHashMap<>();
	private final AtomicReference<Long> lock = new AtomicReference<>(null);
	private final PacketCombiner packetCombiner;
	private final AtomicLong packetId = new AtomicLong();

	public AsyncPacketClient(AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize) throws IOException {
		super(asyncSocket, readBufSize, writeBufSize);
		packetCombiner = new PacketCombiner(1024 * 1024, 16);
		setAsyncClientFactory(new AsyncPacketClientFactory());
		setAsyncConnectHandlerFactory(s -> new DefaultAsyncConnectHandler(s));

		EventManager.registerEvents(this, this);
	}

	public void freeReadBuffer(DistributedByteBuffer dbb) {
		packetCombiner.freeBuffer(dbb);
	}

	@EventHandler
	public void onPacket(ClientPacketReceiveEvent event) {
		Packet packet = event.getPacket();
		PacketCallback pc = null;
		Packet awake;
		pc = packetCallbacks.remove(packet.getTimeStamp());
		awake = awaitResponses.remove(packet.getTimeStamp());
		if (pc != null)
			pc.call(packet.getObject());
		if (awake != null)
			awake.awake(packet);
	}

	public final void write(Packet packet) {
		int i = 1;
		while (packetCallbacks.size() + awaitResponses.size() > 70 && (packet.hasPacketCallback() || packet.isAwaitingRespose())) {
			Sockets.sleepMillis(1);
			if (i++ % 10 == 0 && workers.contains(Thread.currentThread().getId())) {
				exec.submit(() -> write(packet));
				return;
			}
		}
		packet.setLong(packetId.getAndIncrement());
		if (packet.hasPacketCallback()) {
			packetCallbacks.put(packet.getTimeStamp(), packet.getPacketCallback());
		}
		if (packet.isAwaitingRespose())
			awaitResponses.put(packet.getTimeStamp(), packet);
		putWrite(packet.toBytes());
	}

	@Override
	public void onBytesReceive(ByteBuffer b) {
		for (UnfinishedPacket packet : packetCombiner.put(b)) {
			try {
				unfinishedPacketsQueue.put(Tuple.of(this, packet));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onConnect() {
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
}