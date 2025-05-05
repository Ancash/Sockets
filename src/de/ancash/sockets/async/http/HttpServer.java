package de.ancash.sockets.async.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import de.ancash.sockets.async.client.DefaultAsyncConnectHandler;
import de.ancash.sockets.async.server.AbstractAsyncServer;
import de.ancash.sockets.async.server.DefaultAsyncAcceptHandler;
import de.ancash.sockets.async.stream.AsyncStreamReadHandler;

public class HttpServer extends AbstractAsyncServer {

	public static void main(String[] args) throws IOException, InterruptedException {
		HttpServer server = new HttpServer("localhost", 12345);
		server.start();
//		HttpClient client = new HttpClientFactory().newInstance("localhost", 12345, 1024 * 64, 1024 * 64);
//		while(!client.isConnected()) {
//			Sockets.sleepMillis(1);
//		}
//		while(true) {
////			Sockets.sleepMillis(1);
//			Sockets.sleep(10_000);
//			client.write(System.nanoTime() + "\n");
//		}
	}

	private final Set<HttpClient> clients = new HashSet<>();
	private final Executor handler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
			new ThreadFactory() {

				AtomicLong cnt = new AtomicLong();

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "HttpHandler-" + cnt.getAndIncrement());
				}
			});
	private final Map<HttpClient, AtomicBoolean> lock = new ConcurrentHashMap<HttpClient, AtomicBoolean>();
	private final LinkedBlockingQueue<HttpClient> queue = new LinkedBlockingQueue<HttpClient>();
	private final AtomicBoolean running = new AtomicBoolean(true);

	public HttpServer(String address, int port) {
		super(address, port);
		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
			handler.execute(new HttpHandler(lock, queue, running));
		}
		setReadHandlerFactory(s -> new AsyncStreamReadHandler(s, getReadBufSize()));
		setAsyncAcceptHandlerFactory(s -> new DefaultAsyncAcceptHandler(s));
		setAsyncClientFactory(new HttpClientFactory());
		setAsyncConnectHandlerFactory(s -> new DefaultAsyncConnectHandler(s));
	}

	public void queue(HttpClient client) {
		queue.add(client);
	}

	@Override
	public void onAccept(AsynchronousSocketChannel socket) throws IOException {
		HttpClient cl = (HttpClient) getAsyncClientFactory().newInstance(this, socket, getReadBufSize(),
				getWriteBufSize());
		synchronized (clients) {
			clients.add(cl);
		}
		cl.setWriteHandler(writeHandlerFactory.newInstance(cl));
		cl.setConnected(true);
		System.out.println(cl.getRemoteAddress() + " connected! (" + clients.size() + ")");
		/*
		 * new Thread(() -> { AsyncStreamReadHandler streamer = (AsyncStreamReadHandler)
		 * rh; BufferedReader reader = new BufferedReader(new
		 * InputStreamReader(streamer.getInputStream()));
		 * System.out.println("read thread started on " + reader); String remote =
		 * cl.getRemoteAddress().toString(); while (true) { try { String s =
		 * reader.readLine(); System.out.println(remote + "-" + " wrote: " + s); } catch
		 * (IOException e) { e.printStackTrace(); break; } }
		 * System.out.println("reader exit"); }).start();
		 */
		cl.startReadHandler();
	}

	public void onDisconnect(HttpClient cl, Throwable th) {

		synchronized (clients) {
			clients.remove(cl);
		}
		System.out.println(cl.getRemoteAddress() + " disconnected! (" + clients.size() + ")");
	}
}
