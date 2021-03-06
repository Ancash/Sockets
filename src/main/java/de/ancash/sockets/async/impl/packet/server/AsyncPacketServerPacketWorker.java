package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;

import de.ancash.datastructures.tuples.Duplet;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.events.ServerPacketReceiveEvent;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketServerPacketWorker implements Runnable{

	private final AsyncPacketServer serverSocket;
	private final int nr;
	
	public AsyncPacketServerPacketWorker(AsyncPacketServer serverSocket, int nr) {
		this.serverSocket = serverSocket;
		this.nr = nr;
	}
	
	public Duplet<UnfinishedPacket, AsyncPacketServerClient> next() throws InterruptedException {
		return serverSocket.takeUnfishinedPacket();
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName() + "-" + nr);
		while(true) {
			Duplet<UnfinishedPacket, AsyncPacketServerClient> pair = null;
			try {
				pair = next();
			} catch(InterruptedException e) {
				System.err.println("Stopping " + this.getClass().getSimpleName() + ": " + e);
				try {
					serverSocket.stop();
				} catch (IOException e1) {}
				return;
			}
			
			UnfinishedPacket unfinishedPacket = pair.getFirst();
			AsyncPacketServerClient sender = pair.getSecond();
			Packet reconstructed = new Packet(unfinishedPacket.getHeader());
			
			try {
				reconstructed.reconstruct(unfinishedPacket.getBytes());
				switch (unfinishedPacket.getHeader()) {
				case Packet.KEEP_ALIVE_HEADER:
					Packet keepAlive = new Packet(unfinishedPacket.getHeader());
					keepAlive.reconstruct(unfinishedPacket.getBytes());
					keepAlive.setSerializable("You are alive!");
					sender.putWrite(keepAlive.toBytes());
					break;
				case Packet.PING_PONG:
					sender.putWrite(unfinishedPacket.getBytes());
					break;
				case Packet.PING_HEADER:
					Packet ping = new Packet(unfinishedPacket.getHeader());
					ping.reconstruct(unfinishedPacket.getBytes());
					System.out.println(sender.getRemoteAddress() + " pinged in " + (System.nanoTime() - ping.getTimeStamp()) + " ns!");
					break;
				default:
					if(reconstructed.isClientTarget())
						serverSocket.writeAllExcept(reconstructed, sender);
					else
						EventManager.callEvent(new ServerPacketReceiveEvent(reconstructed, sender));
					break;
				}
			} catch(Exception ex) {
				System.err.println("Could not process packet!:");
				ex.printStackTrace();
			}
			
		}
	}
}