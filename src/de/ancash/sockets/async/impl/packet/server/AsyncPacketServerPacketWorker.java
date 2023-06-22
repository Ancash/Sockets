package de.ancash.sockets.async.impl.packet.server;

import java.io.IOException;

import com.lmax.disruptor.EventHandler;

import de.ancash.datastructures.tuples.Duplet;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.async.impl.packet.UnfinishedPacketEvent;
import de.ancash.sockets.events.ServerPacketReceiveEvent;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.UnfinishedPacket;

public class AsyncPacketServerPacketWorker implements Runnable, EventHandler<UnfinishedPacketEvent> {

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
	public void onEvent(UnfinishedPacketEvent event, long sequence, boolean endOfBatch) throws Exception {
		UnfinishedPacket unfinishedPacket = event.packet;
		AsyncPacketServerClient sender = (AsyncPacketServerClient) event.client;
		Packet reconstructed = new Packet(unfinishedPacket.getHeader());
		if (reconstructed.getHeader() == Packet.FILL)
			return;
		try {
			reconstructed.reconstruct(unfinishedPacket.getBytes());
			switch (unfinishedPacket.getHeader()) {
			case Packet.PING_PONG:
				sender.putWrite(unfinishedPacket.getBytes());
				break;
			default:
				if (reconstructed.isClientTarget())
					serverSocket.writeAllExcept(reconstructed, sender);
				else
					EventManager.callEvent(new ServerPacketReceiveEvent(reconstructed, sender));
				break;
			}
		} catch (Throwable ex) {
			System.err.println("Could not process packet!:");
			ex.printStackTrace();
		}
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
			Packet reconstructed = new Packet(unfinishedPacket.getHeader());
			if (reconstructed.getHeader() == Packet.FILL)
				continue;
			try {
				reconstructed.reconstruct(unfinishedPacket.getBytes());
//				System.out.println((System.nanoTime() - reconstructed.getTimeStamp()) / 1000D + " micros to server");
				switch (unfinishedPacket.getHeader()) {
				case Packet.PING_PONG:
					sender.putWrite(unfinishedPacket.getBytes());
					break;
				default:
					if (reconstructed.isClientTarget())
						serverSocket.writeAllExcept(reconstructed, sender);
					else
						EventManager.callEvent(new ServerPacketReceiveEvent(reconstructed, sender));
					break;
				}
			} catch (Throwable ex) {
				System.err.println("Could not process packet!:");
				ex.printStackTrace();
			}

		}
	}
}