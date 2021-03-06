package de.ancash.sockets.async.impl.packet.client;

import java.util.concurrent.ArrayBlockingQueue;

import de.ancash.datastructures.tuples.Duplet;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.events.ClientPacketReceiveEvent;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketClientPacketWorker implements Runnable{

	private final ArrayBlockingQueue<Duplet<AsyncPacketClient, UnfinishedPacket>> queue;
	private int id;
	
	public AsyncPacketClientPacketWorker(ArrayBlockingQueue<Duplet<AsyncPacketClient, UnfinishedPacket>> unfinishedPacketsQueue, int id) {
		this.queue = unfinishedPacketsQueue;
		this.id = id;
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("ClientPacketWorker - " + id);
		while(true) {
			try {
				Duplet<AsyncPacketClient, UnfinishedPacket> unfinishedPacket = queue.take();
				byte[] bytes = unfinishedPacket.getSecond().getBytes();
				Packet p = new Packet(unfinishedPacket.getSecond().getHeader());
				p.reconstruct(bytes);
				EventManager.callEvent(new ClientPacketReceiveEvent(unfinishedPacket.getFirst(), p));
			} catch (InterruptedException e) {
				return;
			}
		}
	}
}
