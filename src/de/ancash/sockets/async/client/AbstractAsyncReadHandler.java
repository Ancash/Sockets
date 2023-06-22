package de.ancash.sockets.async.client;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import de.ancash.sockets.async.ByteEventHandler;

public abstract class AbstractAsyncReadHandler implements CompletionHandler<Integer, ByteBuffer> {

	protected final AbstractAsyncClient client;
	protected final ByteBuffer readBuffer;
	protected ByteEventHandler byteHandler;

	public AbstractAsyncReadHandler(AbstractAsyncClient asyncClient, int readBufSize, ByteEventHandler byteHandler) {
		this.client = asyncClient;
		this.byteHandler = byteHandler;
		this.readBuffer = ByteBuffer.allocate(readBufSize);
	}

	@Override
	public void completed(Integer read, ByteBuffer buf) {
		if (read == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), buf);
			return;
		}
		buf.flip();
		byte[] bytes = new byte[buf.remaining()];
		buf.get(bytes);
		buf.clear();
		if (byteHandler != null)
			byteHandler.onBytes(bytes);
		else
			client.onBytesReceive(bytes);
		client.getAsyncSocketChannel().read(buf, client.timeout, client.timeoutunit, buf, this);
	}

	@Override
	public void failed(Throwable arg0, ByteBuffer arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}
}