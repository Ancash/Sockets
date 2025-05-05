package de.ancash.sockets.async.client;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import de.ancash.sockets.async.server.AbstractAsyncServer;

public abstract class AbstractAsyncClientFactory<T extends AbstractAsyncClient> {

	private static AsynchronousChannelGroup group;
	static {
		try {
			AtomicInteger cnt = new AtomicInteger();
			group = AsynchronousChannelGroup.withFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 4, 1), new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "IO_Worker-" + cnt.getAndIncrement());
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static AsynchronousChannelGroup getGroup() {
		return group;
	}

	public abstract T newInstance(AbstractAsyncServer asyncServer, AsynchronousSocketChannel socket, int readBufSize, int writeBufSize)
			throws IOException;

	public abstract T newInstance(String address, int port, int readBufSize, int writeBufSize) throws IOException;
}