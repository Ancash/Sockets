package de.ancash.sockets.async.client;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.ancash.libs.org.bukkit.event.EventHandler;
import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.libs.org.bukkit.event.Listener;
import de.ancash.sockets.events.ClientConnectEvent;
import de.ancash.sockets.events.ClientDisconnectEvent;

public abstract class AbstractAsyncClientWrapper<S extends AbstractAsyncClient, T extends AbstractAsyncClientFactory<S>>
		implements Listener {

	protected S chatClient;
	protected final T factory;
	protected int readBuf = 256 * 1024;
	protected int writeBuf = 256 * 1024;
	protected int threads = 3;

	@SuppressWarnings("deprecation")
	public AbstractAsyncClientWrapper(Class<T> clazz) {
		EventManager.registerEvents(this, this);
		try {
			this.factory = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("nls")
	public synchronized boolean connect(String address, int port) {
		if (chatClient != null) {
			try {
				chatClient.onDisconnect(new IllegalStateException("Only one client"));
			} catch (Exception ex) {
			}
			chatClient = null;
		}
		try {
			chatClient = factory.newInstance(address, port, readBuf, writeBuf, threads);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public void putWrite(byte[] b) throws InterruptedException {
		chatClient.putWrite(b);
	}

	public void putWrite(ByteBuffer b) throws InterruptedException {
		chatClient.putWrite(b);
	}

//	public boolean offerWrite(byte[] b) {
//		return chatClient.offerWrite(b);
//	}
//
//	public boolean offerWrite(ByteBuffer b) {
//		return chatClient.offerWrite(b);
//	}

	public S getClient() {
		return chatClient;
	}

	@EventHandler
	public void onClientDisconnect(ClientDisconnectEvent event) {
		if (event.getClient().equals(chatClient))
			this.onClientDisconnect(event.getClient());
	}

	@EventHandler
	public void onClientConnect(ClientConnectEvent event) {
		if (event.getClient().equals(chatClient))
			this.onClientConnect(event.getClient());
	}

	public abstract void onClientDisconnect(AbstractAsyncClient client);

	public abstract void onClientConnect(AbstractAsyncClient client);
}