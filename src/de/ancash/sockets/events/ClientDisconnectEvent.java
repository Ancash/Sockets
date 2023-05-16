package de.ancash.sockets.events;

import de.ancash.libs.org.bukkit.event.Event;
import de.ancash.libs.org.bukkit.event.HandlerList;
import de.ancash.sockets.async.client.AbstractAsyncClient;

public class ClientDisconnectEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	private final AbstractAsyncClient client;
	private final Throwable th;

	public ClientDisconnectEvent(AbstractAsyncClient client, Throwable th) {
		this.client = client;
		this.th = th;
	}

	public AbstractAsyncClient getClient() {
		return client;
	}

	public Throwable getThrowable() {
		return th;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}