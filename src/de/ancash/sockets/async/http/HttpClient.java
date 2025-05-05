package de.ancash.sockets.async.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.DefaultAsyncConnectHandler;
import de.ancash.sockets.async.server.AbstractAsyncServer;
import de.ancash.sockets.async.stream.AsyncStreamReadHandler;

public class HttpClient extends AbstractAsyncClient {

	private final HttpServer server;
	private final HttpParser parser;

	public HttpClient(AsynchronousSocketChannel asyncSocket, int readBufSize, int writeBufSize) throws IOException {
		this(null, asyncSocket, readBufSize, writeBufSize);
	}

	public HttpClient(AbstractAsyncServer server, AsynchronousSocketChannel asyncSocket, int readBufSize,
			int writeBufSize) throws IOException {
		super(asyncSocket, readBufSize, writeBufSize);
		this.server = (HttpServer) server;
		setReadHandler(new AsyncStreamReadHandler(this, readBufSize * 4));
		parser = new HttpParser(((AsyncStreamReadHandler) readHandler).getInputStream());
		setAsyncConnectHandlerFactory(s -> new DefaultAsyncConnectHandler(s));
	}

	public HttpRequest parseRequest() throws IOException {
		return parser.parseRequest();
	}

	@Override
	public void onBytesReceive(ByteBuffer bytes) {
		if (server != null) {
			server.queue(this);
		}
	}

	@Override
	public boolean isConnectionValid() {
		return isConnected();
	}

	@Override
	public void onConnect() {

	}

	public void write(String s) throws InterruptedException {
		putWrite(s.getBytes());
	}

	@Override
	public void onDisconnect(Throwable th) {
		try {
			getAsyncSocketChannel().close();
		} catch (IOException e) {

		} finally {
			if (server != null) {
				server.onDisconnect(this, th);
			}
		}
	}

}
