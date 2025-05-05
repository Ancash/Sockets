package de.ancash.sockets.async.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpHandler implements Runnable {

	private final Map<HttpClient, AtomicBoolean> lock;
	private final LinkedBlockingQueue<HttpClient> queued;
	private final AtomicBoolean running;

	public HttpHandler(Map<HttpClient, AtomicBoolean> lock, LinkedBlockingQueue<HttpClient> queued,
			AtomicBoolean running) {
		this.lock = lock;
		this.running = running;
		this.queued = queued;
	}

	@Override
	public void run() {
		while (running.get()) {
			HttpClient client;
			try {
				client = queued.take();
			} catch (InterruptedException e) {
				return;
			}
			if (!lock.computeIfAbsent(client, k -> new AtomicBoolean(false)).compareAndSet(false, true)) {
				continue;
			}
			try {
				HttpRequest req = client.parseRequest();
				System.out.println(client.getLocalAddress() + " request: " + req.getStatus() + ", " + req.getRequestURL() + " " + req.getVersion());
				System.out.println(req.getHeaders());
				System.out.println(req.getParams());
			
			} catch (IOException e) {
				client.onDisconnect(e);
			} finally {
				lock.get(client).set(false);
			}
		}
	}

}
