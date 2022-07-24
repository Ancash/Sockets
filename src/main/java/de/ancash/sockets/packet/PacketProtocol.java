package de.ancash.sockets.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.ancash.sockets.utils.BitUtils;
import de.ancash.sockets.utils.Compressor;
import de.ancash.sockets.utils.VarUtils;

class PacketProtocol {

	private final Compressor compressor = new Compressor();
	private boolean useCompression = false;
	private byte info = 0;
	
	public boolean useCompression() {
		return useCompression;
	}

	public void useCompression(boolean useCompression) {
		this.useCompression = useCompression;
		if(useCompression)
			info = BitUtils.setBit(info, 0);
		else
			info = BitUtils.unsetBit(info, 0);
	}

	public void setCompressionLevel(int compressionLevel) {
		compressor.setLevel(compressionLevel);
	}

	public void setCompressionStrategy(int compressionStrategy) {
		compressor.setStrategy(compressionStrategy);
	}	
	
	public byte[] apply(byte[] in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if(useCompression)
			in = compressor.compress(in);
		byte[] time = VarUtils.writeVarLong(System.nanoTime());
		out.write(VarUtils.writeVarInt(in.length + 1 + time.length));
		out.write(info);
		out.write(time);
		return out.toByteArray();
	}
}