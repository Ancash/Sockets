package de.ancash.sockets.utils;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

public class Compressor {

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final byte[] buffer;
	private final Deflater deflater = new Deflater();

	public Compressor() {
		this(8 * 1024);
	}

	public Compressor(int buf) {
		this.buffer = new byte[buf];
	}

	public byte[] compress(byte[] in) {
		deflater.setInput(in);
		deflater.finish();

		while (!deflater.finished())
			baos.write(buffer, 0, deflater.deflate(buffer));

		deflater.reset();
		byte[] out = baos.toByteArray();
		baos.reset();
		return out;
	}

	public void setLevel(int level) {
		deflater.setLevel(level);
	}

	public void setStrategy(int strategy) {
		deflater.setStrategy(strategy);
	}
}