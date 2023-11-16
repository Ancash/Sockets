package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;

import de.ancash.datastructures.tuples.Duplet;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.events.ServerPacketReceiveEvent;
import de.ancash.sockets.io.PositionedByteBuf;
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
		Thread.currentThread().setName("PW" + "-" + nr);
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
				PositionedByteBuf pbb = unfinishedPacket.getBuffer();
				int startPos = pbb.get().position();
				switch (unfinishedPacket.getHeader()) {
				case Packet.PING_PONG:
					sender.putWrite(unfinishedPacket.getBuffer().get());
					break;
				default:
					reconstructed.reconstruct(unfinishedPacket.getBuffer().get());
					pbb.get().position(startPos);
					if (reconstructed.isClientTarget())
						serverSocket.writeAllExcept(reconstructed, sender);
					else
						EventManager.callEvent(new ServerPacketReceiveEvent(reconstructed, sender));
					break;
				}
			} catch (Throwable ex) {
				System.err.println("Could not process packet!:");
				ex.printStackTrace();
			} finally {
				sender.packetCombiner.unblockBuffer(unfinishedPacket.getBuffer());
			}

		}
	}
}