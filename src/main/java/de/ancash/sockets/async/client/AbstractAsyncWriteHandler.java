package de.ancash.sockets.async.client;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

public abstract class AbstractAsyncWriteHandler implements CompletionHandler<Integer, ByteBuffer>{

	protected final AbstractAsyncClient client;
	private boolean canWrite = true;
	private Object writeLock = new Object();
	
	public AbstractAsyncWriteHandler(AbstractAsyncClient asyncSocket) {
		this.client = asyncSocket;
	}
	
	@Override
	public void completed(Integer written, ByteBuffer bb) {
		if(written == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), bb);
			return;	
		}
				
		if(bb.hasRemaining()) {
			client.getAsyncSocketChannel().write(bb, bb, this);
			return;
		} else {
			synchronized (writeLock) {
				canWrite = true;
			}
			client.checkWrite();
		}
	}
	
	@Override
	public void failed(Throwable arg0, ByteBuffer arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}
	
	public boolean canWrite() {
		synchronized (writeLock) {
			return canWrite;
		}
	}

	public boolean write(ByteBuffer bb) {
		synchronized (writeLock) {
			if(!canWrite)
				return false;
			canWrite = false;
		}
		client.getAsyncSocketChannel().write(bb, bb, this);
		return true;
	}
}