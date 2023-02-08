package de.ancash.sockets.utils;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Decompressor {

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final byte[] buffer;
	private final Inflater inflater = new Inflater();
	
	public Decompressor() {
		this(8 * 1024);
	}
	
	public Decompressor(int buf) {
		this.buffer = new byte[buf];
	}
	
	public byte[] deflate(byte[] in) throws DataFormatException {
		 inflater.setInput(in);
		 while (!inflater.finished())
	       baos.write(buffer, 0, inflater.inflate(buffer));
	     inflater.reset();
	     byte[] out = baos.toByteArray();
	     baos.reset();
	     return out;
	}
	
}