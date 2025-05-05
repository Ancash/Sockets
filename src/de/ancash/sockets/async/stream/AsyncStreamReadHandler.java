package de.ancash.sockets.async.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import de.ancash.sockets.async.client.AbstractAsyncClient;
import de.ancash.sockets.async.client.DefaultAsyncReadHandler;

public class AsyncStreamReadHandler extends DefaultAsyncReadHandler{

	private final AsyncInputStream streamer;
	private final int batchSize;
	
	public AsyncStreamReadHandler(AbstractAsyncClient asyncClient, int readBufSize)  {
		super(asyncClient, readBufSize, null);
		streamer = new AsyncInputStream(asyncClient.getReadBufSize());
		byteHandler = this::onBytes;
		batchSize = readBufSize / 4;
	}

	private void onBytes(ByteBuffer buffer) {
		byte[] read = new byte[buffer.remaining()];
		buffer.get(read);
		try {
			int last = 0;
			while((last + 1) * batchSize < read.length) {
				streamer.pipedOut.write(read, last * batchSize, batchSize);	
//				System.out.println("wrote " + batchSize);
				streamer.pipedOut.flush();
				client.onBytesReceive(null);
				last++;
			}
			if(last * batchSize < read.length) {
				streamer.pipedOut.write(read, last, read.length - last * batchSize);
//				System.out.println("wrote " + (read.length - last * batchSize));
				streamer.pipedOut.flush();
				client.onBytesReceive(null);
			}
			
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public InputStream getInputStream() {
		return streamer.getInputStream();
	}
}
