package de.ancash.sockets.async.impl.packet.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import de.ancash.Sockets;
import de.ancash.datastructures.tuples.Duplet;
import de.ancash.datastructures.tuples.Tuple;
import de.ancash.libs.org.bukkit.event.EventHandler;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.libs.org.bukkit.event.Listener;
import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.events.ClientConnectEvent;
import de.ancash.sockets.events.ClientDisconnectEvent;
import de.ancash.sockets.events.ClientPacketReceiveEvent;
import de.ancash.sockets.io.PositionedByteBuf;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCallback;
import de.ancash.sockets.packet.PacketCombiner;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketClient extends AbstractAsyncClient implements Listener {

	private final Map<Long, PacketCallback> packetCallbacks = new HashMap<>();
	private final Map<Long, Packet> awaitResponses = new HashMap<>();
	private final AtomicReference<Long> lock = new AtomicReference<>(null);
	private final PacketCombiner packetCombiner;
	private ExecutorService clientThreadPool = Executors.newCachedThreadPool();
	private ArrayBlockingQueue<Duplet<AsyncPacketClient, UnfinishedPacket>> unfinishedPacketsQueue = new ArrayBlockingQueue<>(
			1000);
	private final int worker;
	private AsynchronousChannelGroup asyncChannelGroup;

	public AsyncPacketClient(AsynchronousSocketChannel asyncSocket, AsynchronousChannelGroup asyncChannelGroup,
			int readBufSize, int writeBufSize, int worker) throws IOException {
		super(asyncSocket, readBufSize, writeBufSize);
		packetCombiner = new PacketCombiner(1024 * 128, readBufSize);
		this.asyncChannelGroup = asyncChannelGroup;
		setAsyncClientFactory(new AsyncPacketClientFactory());
		setAsyncReadHandlerFactory(new AsyncPacketClientReadHandlerFactory());
		setAsyncWriteHandlerFactory(new AsyncPacketClientWriteHandlerFactory());
		setAsyncConnectHandlerFactory(new AsyncPacketClientConnectHandlerFactory());
		setHandlers();
		this.worker = worker;
	}

	@EventHandler
	public void onPacket(ClientPacketReceiveEvent event) {
		Packet packet = event.getPacket();
		try {
			while (!lock.compareAndSet(null, Thread.currentThread().getId())
					&& !lock.compareAndSet(Thread.currentThread().getId(), Thread.currentThread().getId()))
				Sockets.sleep(10_000);
			Optional.ofNullable(packetCallbacks.remove(packet.getTimeStamp()))
					.ifPresent(sc -> sc.call(packet.getObject()));
			Optional.ofNullable(awaitResponses.remove(packet.getTimeStamp())).ifPresent(await -> await.awake(packet));
		} finally {
			lock.set(null);
		}

	}

	public final void write(Packet packet) throws InterruptedException {
		try {
			while (!lock.compareAndSet(null, Thread.currentThread().getId())
					&& !lock.compareAndSet(Thread.currentThread().getId(), Thread.currentThread().getId()))
				Sockets.sleep(10_000);
			packet.addTimeStamp();
			while (packetCallbacks.containsKey(packet.getTimeStamp())
					|| awaitResponses.containsKey(packet.getTimeStamp()))
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
		for (int i = 0; i < worker; i++)
			clientThreadPool.submit(new AsyncPacketClientPacketWorker(unfinishedPacketsQueue, i + 1));
		EventManager.callEvent(new ClientConnectEvent(this));
	}

	@Override
	public synchronized void onDisconnect(Throwable th) {
		if (asyncChannelGroup == null)
			return;
		try {
			asyncChannelGroup.shutdownNow();
			getAsyncSocketChannel().close();
		} catch (IOException e) {

		}
		clientThreadPool.shutdownNow();
		clientThreadPool = null;
		EventManager.callEvent(new ClientDisconnectEvent(this, th));
		asyncChannelGroup = null;
		try {
			while (!lock.compareAndSet(null, Thread.currentThread().getId())
					&& !lock.compareAndSet(Thread.currentThread().getId(), Thread.currentThread().getId()))
				Sockets.sleep(10_000);
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

	public void unblockBuffer(PositionedByteBuf buffer) {
		packetCombiner.unblockBuffer(buffer);
	}
}