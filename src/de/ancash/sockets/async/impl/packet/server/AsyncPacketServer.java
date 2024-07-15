package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import de.ancash.datastructures.tuples.Duplet;
import de.ancash.datastructures.tuples.Tuple;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.async.impl.packet.client.AsyncPacketClientWriteHandlerFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;
import de.ancash.sockets.events.ClientConnectEvent;
import de.ancash.sockets.events.ClientDisconnectEvent;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketServer extends AbstractAsyncServer {

	private final LinkedBlockingQueue<Duplet<UnfinishedPacket, AsyncPacketServerClient>> unfishedPackets = new LinkedBlockingQueue<>(10_000);
	private final ExecutorService workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private final Set<AsyncPacketServerClient> clients = new HashSet<>();

	public AsyncPacketServer(String address, int port, int packetWorker) {
		super(address, port);
		setReadBufSize(1024 * 32);
		setWriteBufSize(1024 * 32);
		setAsyncAcceptHandlerFactory(new AsyncPacketServerAcceptHandlerFactory());
		setAsyncReadHandlerFactory(new AsyncPacketServerReadHandlerFactory(this));
		setAsyncWriteHandlerFactory(new AsyncPacketClientWriteHandlerFactory());
		setAsyncClientFactory(new AsyncPacketServerClientFactory());
		for (int i = 0; i < packetWorker; i++)
			workerPool.submit(new AsyncPacketServerPacketWorker(this, i));
	}

	protected final void onPacket(UnfinishedPacket unfinishedPacket, AsyncPacketServerClient sender) {
		try {
			unfishedPackets.put(Tuple.of(unfinishedPacket, sender));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void stop() throws IOException {
		super.stop();
		workerPool.shutdownNow();
	}

	@Override
	public void onAccept(AsynchronousSocketChannel socket) throws IOException {
		AsyncPacketServerClient cl = (AsyncPacketServerClient) getAsyncClientFactory().newInstance(this, socket, getReadBufSize(), getWriteBufSize());
		synchronized (clients) {
			clients.add(cl);
		}
		cl.startReadHandler();
		System.out.println(cl.getRemoteAddress() + " connected!");
		EventManager.callEvent(new ClientConnectEvent(cl));
	}

	public void onDisconnect(AsyncPacketServerClient cl, Throwable th) {
		System.out.println(cl.getRemoteAddress() + " disconnected!");
		synchronized (clients) {
			clients.remove(cl);
		}
		EventManager.callEvent(new ClientDisconnectEvent(cl, th));
	}

	public Duplet<UnfinishedPacket, AsyncPacketServerClient> takeUnfishinedPacket() throws InterruptedException {
		return unfishedPackets.take();
	}

	public void writeAllExcept(Packet reconstructed, AsyncPacketServerClient sender) throws InterruptedException {
		synchronized (clients) {
			for (AsyncPacketServerClient cl : clients)
				if (!cl.equals(sender))
					cl.putWrite(reconstructed.toBytes());
		}
	}

	public boolean delayNextRead() {
		return unfishedPackets.remainingCapacity() < 1000;
	}
}