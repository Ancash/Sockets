package de.ancash.sockets.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.ancash.misc.ConversionUtil;

public class PacketCombiner {

	static AtomicInteger id = new AtomicInteger();

	static byte get(ByteBuffer bb, int index) {
		return 0;
	}

	public static void main(String[] args) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IOException {
//		Packet packet = new Packet(Packet.PING_PONG);
//		int pl = 1024 * 127;
//		byte[] a = new byte[pl];
//		packet.setObject(a);
//		ByteBuffer bb = packet.toBytes();
//		System.out.println(bb.capacity());
//		System.out.println(bb.limit());
//		List<ByteBuffer> list = new ArrayList<>();
//		int bufSize = 1024 * 16;
//		if (bb.remaining() > bufSize) {
//			while (bb.hasRemaining()) {
//				ByteBuffer dup = bb.slice();
//				dup.limit(Math.min(bb.remaining(), bufSize));
//				bb.position(bb.position() + Math.min(bb.remaining(), bufSize));
//				list.add(dup);
//
//			}
//		} else {
//			list.add(bb);
//		}
//		PacketCombiner pc = new PacketCombiner();
//		long now = System.nanoTime();
//		List<UnfinishedPacket> o = null;
//		for (int i = 0; i < 100_000; i++)
//			for (ByteBuffer b : list) {
//				o = pc.put(b);
//				if (!o.isEmpty())
//					pc.bufferBuffer.unblockBuffer(o.get(0).buffer);
//			}
//		System.out.println(System.nanoTime() - now + " ns");
//		System.out.println((System.nanoTime() - now) / 100_000 + " ns/iter");
//		Packet rec = new Packet(o.get(0).getHeader());
//		rec.reconstruct(o.get(0).buffer.get());
		Packet packet = new Packet(Packet.PING_PONG);
		packet.setAwaitResponse(true);
		packet.isClientTarget(false);
		packet.setObject(System.nanoTime());
		ByteBuffer bb = packet.toBytes();
		ByteBuffer o = ByteBuffer.allocate(bb.remaining() * 100);
		for (int i = 0; i < 100; i++) {
			o.put(packet.toBytes());
		}
		o.flip();
		System.out.println(ConversionUtil.bytesToHex(o.array()));
		System.out.println(o.limit());
		System.out.println(o.remaining());
		System.out.println(o.position());
		System.out.println(o.capacity());
//		PacketCombiner pc = new PacketCombiner(1024 * 128);
//		try {
//			System.out.println(pc.put(o).size());
//		} catch (Throwable th) {
//			th.printStackTrace();
//		}
	}

	final int iid = id.getAndIncrement();
	private int arrPos = 4;
	private byte[] sizeBytes = new byte[4];
	private ByteBuffer buffer;

	private int size;

	private boolean hasSize = false;
	private final int maxSize;
	private int added = 0;

	public PacketCombiner(int maxSize) {
		this.maxSize = maxSize;
	}

	@SuppressWarnings("nls")
	public synchronized List<UnfinishedPacket> put(ByteBuffer buf) {
		List<UnfinishedPacket> restored = new ArrayList<>();
		for (int pos = 0; pos < buf.limit();) {
			if (!hasSize) {
				sizeBytes[added] = buf.get(pos);
				added++;
				if (added == 4) {
					size = SerializationUtil.bytesToInt(sizeBytes);
					if (size <= 0 || size > maxSize)
						throw new IllegalArgumentException("invalid size: " + size);
					hasSize = true;
					buffer = ByteBuffer.allocate(size);
					buffer.limit(size);
					added = 0;
					buffer.put(sizeBytes);
				}
				pos++;
				continue;
			}
			int canAdd = buffer.limit() - buffer.position();
			canAdd = Math.min(size - arrPos, canAdd);
			canAdd = Math.min(buf.limit() - pos, canAdd);
			int willAdd = canAdd;
			buf.position(pos);
			int oldLimit = buf.limit();
			buf.limit(pos + willAdd);
			buffer.put(buf);
			buf.limit(oldLimit);
			arrPos += willAdd;
			pos += willAdd;

			if (arrPos == size) {
				UnfinishedPacket up = new UnfinishedPacket().setHeader(SerializationUtil.bytesToShort(new byte[] { buffer.get(4), buffer.get(5) }));
				buffer.position(0);
				up.buffer = buffer;
				buffer = null;
				restored.add(up);
				hasSize = false;
				arrPos = 4;
			}
		}
		return restored;
	}

	public int getMaxSize() {
		return maxSize;
	}
}
