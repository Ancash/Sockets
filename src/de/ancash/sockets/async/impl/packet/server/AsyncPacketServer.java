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
			1000);
	private final ExecutorService workerPool = IThreadPoolExecutor
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private final Set<AsyncPacketServerClient> clients = new HashSet<>();
//	private final DelegatingLoadBalancingMultiConsumerDisruptor<UnfinishedPacketEvent> worker;

	public AsyncPacketServer(String address, int port, int packetWorker) {
		super(address, port);
//		worker = new DelegatingLoadBalancingMultiConsumerDisruptor<UnfinishedPacketEvent>(UnfinishedPacketEvent::new,
//				1024 * 2, ProducerType.MULTI, new SleepingWaitStrategy(0, 1),
//				IntStream.range(0, packetWorker).boxed().map(i -> new AsyncPacketServerPacketWorker(this, i + 1))
//						.toArray(AsyncPacketServerPacketWorker[]::new));
		setReadBufSize(1024 * 16);
		setWriteBufSize(1024 * 16);
		setAsyncAcceptHandlerFactory(new AsyncPacketServerAcceptHandlerFactory());
		setAsyncReadHandlerFactory(new AsyncPacketServerReadHandlerFactory(this));
		setAsyncWriteHandlerFactory(new AsyncPacketClientWriteHandlerFactory());
		setAsyncClientFactory(new AsyncPacketServerClientFactory());
		for (int i = 0; i < packetWorker; i++)
			workerPool.submit(new AsyncPacketServerPacketWorker(this, i + 1));
	}

	protected final void onPacket(UnfinishedPacket unfinishedPacket, AsyncPacketServerClient sender) {
		unfishedPackets.offer(Tuple.of(unfinishedPacket, sender));
//		System.out.println("publishing up");
//		worker.publishEvent((e, seq) -> {
//			e.client = sender;
//			e.packet = unfinishedPacket;
//		});
//		System.out.println("published up");
	}

	@Override
	public synchronized void stop() throws IOException {
		super.stop();
		workerPool.shutdownNow();
	}

	@Override
	public void onAccept(AsynchronousSocketChannel socket) throws IOException {
		AsyncPacketServerClient cl = (AsyncPacketServerClient) getAsyncClientFactory().newInstance(this, socket,
				getReadBufSize(), getWriteBufSize());
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