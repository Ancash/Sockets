package de.ancash.sockets.async.client;

public interface IReadHandler {

	public boolean tryInitRead();

	public void onDisconnect();
}
