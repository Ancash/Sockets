package de.ancash.sockets.events;

import de.ancash.libs.org.bukkit.event.Event;
import de.ancash.libs.org.bukkit.event.HandlerList;
import de.ancash.sockets.async.client.AbstractAsyncClient;

public class ClientConnectEvent extends Event{

	private static final HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
        return handlers;
    }
	
	private final AbstractAsyncClient cl;
	
	public ClientConnectEvent(AbstractAsyncClient cl) {
		this.cl = cl;
	}
	
	public AbstractAsyncClient getClient() {
		return cl;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}