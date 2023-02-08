package de.ancash.sockets.events;

import de.ancash.libs.org.bukkit.event.Event;
import de.ancash.libs.org.bukkit.event.HandlerList;
import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.packet.Packet;

public final class ClientPacketReceiveEvent extends Event{
	
	private static final HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
        return handlers;
    }
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	private final Packet packet;
	private final AbstractAsyncClient receiver;
	
	public ClientPacketReceiveEvent(AbstractAsyncClient receiver, Packet packet) {
		this.packet = packet;
		this.receiver = receiver;
	}
	
	public AbstractAsyncClient getReceiver() {
		return receiver;
	}
	
	public Packet getPacket() {
		return packet;
	}
}
