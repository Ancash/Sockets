package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

import de.ancash.datastructures.tuples.Duplet;
import de.ancash.datastructures.tuples.Tuple;
import de.ancash.ithread.IThreadPoolExecutor;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.async.impl.packet.client.AsyncPacketClientWriteHandlerFactory;
import de.ancash.sockets.async.server.AbstractAsyncServer;
import de.ancash.sockets.events.ClientConnectEvent;
import de.ancash.sockets.events.ClientDisconnectEvent;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketServer extends AbstractAsyncServer {

	private final ArrayBlockingQueue<Duplet<UnfinishedPacket, AsyncPacketServerClient>> unfishedPackets = new ArrayBlockingQueue<>(
			10000);
	private final ExecutorService workerPool = IThreadPoolExecutor
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private final Set<AsyncPacketServerClient> clients = new HashSet<>();

	public AsyncPacketServer(String address, int port, int packetWorker) {
		super(address, port);
		setReadBufSize(1024 * 256);
		setWriteBufSize(1024 * 256);
		setAsyncAcceptHandlerFactory(new AsyncPacketServerAcceptHandlerFactory());
		setAsyncReadHandlerFactory(new AsyncPacketServerReadHandlerFactory(this));
		setAsyncWriteHandlerFactory(new AsyncPacketClientWriteHandlerFactory());
		setAsyncClientFactory(new AsyncPacketServerClientFactory());
		for (int i = 0; i < packetWorker; i++)
			workerPool.submit(new AsyncPacketServerPacketWorker(this, i + 1));
	}

	protected final void onPacket(UnfinishedPacket unfinishedPacket, AsyncPacketServerClient sender) {
		unfishedPackets.offer(Tuple.of(unfinishedPacket, sender));
	}

	@Override
	public void onAccept(AsynchronousSocketChannel socket) throws IOException {
		AsyncPacketServerClient cl = (AsyncPacketServerClient) getAsyncClientFactory().newInstance(this, socket,
				getWriteQueueSize(), getReadBufSize(), getWriteBufSize());
		synchronized (clients) {
			clients.add(cl);
		}
		System.out.println(cl.getRemoteAddress() + " connected!");
		cl.startReadHandler();
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

	public void writeAllExcept(Packet reconstructed, AsyncPacketServerClient sender) {
		ByteBuffer bb = reconstructed.toBytes();
		synchronized (clients) {
			clients.stream().filter(client -> !client.equals(sender)).forEach(c -> c.putWrite(bb));
		}
	}
}