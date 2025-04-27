package de.ancash.sockets.async.impl.packet;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.packet.UnfinishedPacket;

public class UnfinishedPacketEvent {

	public UnfinishedPacket packet;
	public AbstractAsyncClient client;
}
