package de.ancash.cli;

import de.ancash.libs.org.bukkit.event.Event;
import de.ancash.libs.org.bukkit.event.HandlerList;

public class CLIEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	private final String s;
	
	public CLIEvent(String s) {
		this.s = s;
	}

	public String getInput() {
		return s;
	}
}