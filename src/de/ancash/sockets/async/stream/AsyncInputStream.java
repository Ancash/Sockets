package de.ancash.sockets.async.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class AsyncInputStream {

	public static void main(String[] args) throws IOException {
		String content = "I'm going through the pipe.\r\n";

		AsyncInputStream stream = new AsyncInputStream(1024 * 16);
		stream.pipedOut.write(content.getBytes());
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream.getInputStream()));
		System.out.println(reader.readLine());
	}

	protected final PipedOutputStream pipedOut;
	protected final PipedInputStream pipedIn = new PipedInputStream();

	public AsyncInputStream(int size) {
		try {
			this.pipedOut = new PipedOutputStream(pipedIn);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public InputStream getInputStream() {
		return pipedIn;
	}
}
