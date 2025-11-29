package de.ancash.sockets.async.impl.packet.netty;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import de.ancash.Sockets;
import de.ancash.sockets.netty.NettyClient;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCallback;

public class NettyPacketClient {


	private final Map<Long, PacketCallback> packetCallbacks = new ConcurrentHashMap<>();
	private final Map<Long, Packet> awaitResponses = new ConcurrentHashMap<>();
	private final AtomicReference<Long> lock = new AtomicReference<>(null);
	private final AtomicLong packetId = new AtomicLong();
	private final NettyClient nettyClient;
	private Consumer<Packet> onPacket;
	private Runnable onDisconnect;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		new NettyPacketClient("localhost", 25000, p -> System.out.println("p"), () -> System.out.println("c"), () -> System.out.println("d"));
	}

	public NettyPacketClient(String address, int port, Consumer<Packet> onPacket, Runnable onConnect, Runnable onDisconnect) {
		this.nettyClient = new NettyClient(address, port, p -> onPacket(p));
		this.onPacket = onPacket;
		this.onDisconnect = onDisconnect;
		nettyClient.setOnDisconnect(() -> onDisconnect());
		nettyClient.setOnConnect(onConnect);
	}
	
	public boolean connect() throws InterruptedException {
		return nettyClient.connect();
	}
	
	private void onPacket(Packet packet) {
		PacketCallback pc = null;
		Packet awake;
		pc = packetCallbacks.remove(packet.getTimeStamp());
		awake = awaitResponses.remove(packet.getTimeStamp());
		if (pc != null)
			pc.call(packet.getObject());
		if (awake != null)
			awake.awake(packet);
		onPacket.accept(packet);
	}

	public final void write(Packet packet) {
		while (packetCallbacks.size() + awaitResponses.size() > 70 && (packet.hasPacketCallback() || packet.isAwaitingRespose())) {
			Sockets.sleepMillis(1);
		}
		packet.setLong(packetId.getAndIncrement());
		if (packet.hasPacketCallback()) {
			packetCallbacks.put(packet.getTimeStamp(), packet.getPacketCallback());
		}
		if (packet.isAwaitingRespose())
			awaitResponses.put(packet.getTimeStamp(), packet);
		nettyClient.write(packet);
	}

	private synchronized void onDisconnect() {
		try {
			while (!lock.compareAndSet(null, Thread.currentThread().getId())
					&& !lock.compareAndSet(Thread.currentThread().getId(), Thread.currentThread().getId()))
				Sockets.sleepMillis(1);
			packetCallbacks.clear();
			awaitResponses.clear();
		} finally {
			lock.set(null);
		}
		onDisconnect.run();
	}

	public void disconnect() {
		nettyClient.disconnect();
	}
	
	public boolean isConnected() {
		return nettyClient.isConnected();
	}
}