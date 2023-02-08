package de.ancash.sockets.async.impl.packet.client;

import java.util.UUID;

import de.ancash.libs.org.bukkit.event.EventHandler;
import de.ancash.sockets.async.client.AbstractAsyncClientWrapper;
import de.ancash.sockets.events.ClientPacketReceiveEvent;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketFuture;

public abstract class AbstractAsyncPacketClientWrapper extends AbstractAsyncClientWrapper<AsyncPacketClient, AsyncPacketClientFactory>{

	public AbstractAsyncPacketClientWrapper() {
		super(AsyncPacketClientFactory.class);
	}

	public PacketFuture sendPacket(Packet packet) {
		return sendPacket(packet, null);
	}
	
	public PacketFuture sendPacket(Packet packet, UUID uuid) {
		chatClient.write(packet);
		return new PacketFuture(packet, uuid);
	}
	
	@EventHandler
	public void onPacket(ClientPacketReceiveEvent event) {
		if(event.getReceiver().equals(chatClient))
			this.onPacketReceive(event.getPacket());
	}
	
	public abstract void onPacketReceive(Packet packet);
}