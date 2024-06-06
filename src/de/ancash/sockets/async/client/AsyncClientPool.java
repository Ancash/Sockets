package de.ancash.sockets.async.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.ancash.sockets.packet.Packet;

public class AsyncClientPool<S extends AbstractAsyncClient, T extends AbstractAsyncClientFactory<S>> implements Runnable {

	private final AsyncClientPoolWatcher watcher = new AsyncClientPoolWatcher();
	private final String address;
	private final int port;
	private final T factory;
	private final int connections;
	private final int readBufSize;
	private final int writeBufSize;
	private final int threadsPerCon;
	private boolean enabled = false;
	private final Set<S> clients = new HashSet<>();

	public AsyncClientPool(Class<T> tClazz, String address, int port, int connections, int readBufSize, int writeBufSize, int threadsPerCon) {
		try {
			this.factory = tClazz.getConstructor().newInstance();
			this.port = port;
			this.address = address;
			this.connections = connections;
			this.readBufSize = readBufSize;
			this.writeBufSize = writeBufSize;
			this.threadsPerCon = threadsPerCon;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	public void start() {
		stop();
		enabled = true;
		for (int i = 0; i < connections; i++)
			newConnection();
	}

	public boolean newConnection() {
//		try {
//			S newCon = factory.newInstance(address, port, readBufSize, writeBufSize, threadsPerCon);
//			int i = 0;
//			while (!newCon.isConnected()) {
//				Thread.sleep(1);
//				i++;
//				if (i >= 1000)
//					throw new IOException("Connection refused");
//			}
//
//			synchronized (clients) {
//				clients.add(newCon);
//				System.out.println("Established new connection to " + address + ":" + port + " (" + clients.size() + "/" + connections + ")");
//			}
//			return true;
//		} catch (IOException | InterruptedException e) {
//			System.err.println("Could not establish connection to " + address + ":" + port + " (" + e + ")");
//			return false;
//		}
		return false;
	}

	public boolean write(byte[] b) {
		return this.write(ByteBuffer.wrap(b));
	}

	public boolean write(ByteBuffer bb) {
		if (!enabled)
			return false;
		synchronized (clients) {
//			clients.stream().sorted((a, b) -> Integer.compare(a.getWritingQueueSize(), b.getWritingQueueSize())).findFirst().get().putWrite(bb);
		}
		return true;
	}

	public boolean write(Packet packet) {
		if (!enabled)
			return false;
		synchronized (clients) {
//			AbstractAsyncClient client = clients.stream().sorted((a, b) -> Integer.compare(a.getWritingQueueSize(), b.getWritingQueueSize()))
//					.findFirst().get();
//			client.putWrite(packet.toBytes());
			return true;
		}
	}

	public void stop() {
		synchronized (clients) {
			if (!enabled)
				return;
			enabled = false;
			clients.forEach(client -> {
				client.setConnected(false);
				client.onDisconnect(null);
			});
			clients.clear();
		}
	}

	@Override
	public void run() {
		watcher.run();
	}

	class AsyncClientPoolWatcher implements Runnable {

		@Override
		public void run() {
			while (enabled) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}

				synchronized (clients) {
					for (AbstractAsyncClient client : clients.stream().collect(Collectors.toSet())) {
						if (client.isConnected())
							continue;
						clients.remove(client);
					}
				}
				int fails = 0;
				while (clients.size() < connections) {
					if (!newConnection())
						fails++;
					if (fails >= 3)
						break;
				}
			}
			stop();
		}
	}
}