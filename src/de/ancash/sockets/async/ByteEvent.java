package de.ancash.sockets.async;

import de.ancash.sockets.async.client.AbstractAsyncClient;

public class ByteEvent {

	public byte[] bytes;
	public AbstractAsyncClient client;
	public Runnable r;
}