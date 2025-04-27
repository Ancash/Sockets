package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;

import de.ancash.datastructures.tuples.Duplet;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.events.ServerPacketReceiveEvent;
import de.ancash.sockets.io.DistributedByteBuffer;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketServerPacketWorker implements Runnable {

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
		Thread.currentThread().setName("ServerPacketWorker-" + nr);
		while (true) {
			Duplet<UnfinishedPacket, AsyncPacketServerClient> pair = null;
			try {
				pair = next();
			} catch (InterruptedException e) {
				System.err.println("Stopping " + this.getClass().getSimpleName() + ": " + e);
				try {
					serverSocket.stop();
				} catch (IOException e1) {
				}
				return;
			}

			UnfinishedPacket unfinishedPacket = pair.getFirst();
			AsyncPacketServerClient sender = pair.getSecond();
			try {
				Packet reconstructed = new Packet(unfinishedPacket.getHeader());
				DistributedByteBuffer pbb = unfinishedPacket.buffer;
				int startPos = pbb.buffer.position();
				switch (unfinishedPacket.getHeader()) {
				case Packet.PING_PONG:
					sender.putWrite(unfinishedPacket.buffer.buffer);
					break;
				default:
					reconstructed.reconstruct(unfinishedPacket.buffer.buffer);
					pbb.buffer.position(startPos);
					if (reconstructed.isClientTarget())
						serverSocket.writeAllExcept(reconstructed, sender);
					else
						EventManager.callEvent(new ServerPacketReceiveEvent(reconstructed, sender));
					break;
				}
				sender.freeReadBuffer(pbb);
			} catch (Throwable ex) {
				System.err.println("Could not process packet!:");
				ex.printStackTrace();
			}

		}
	}
}