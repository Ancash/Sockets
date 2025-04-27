package de.ancash.sockets.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.ancash.misc.ConversionUtil;
import de.ancash.sockets.io.ByteBufferDistributor;
import de.ancash.sockets.io.DistributedByteBuffer;

public class PacketCombiner {

	static AtomicInteger id = new AtomicInteger();

//	public static void main(String[] args) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IOException {
////		Packet packet = new Packet(Packet.PING_PONG);
////		int pl = 1024 * 127;
////		byte[] a = new byte[pl];
////		packet.setObject(a);
////		ByteBuffer bb = packet.toBytes();
////		System.out.println(bb.capacity());
////		System.out.println(bb.limit());
////		List<ByteBuffer> list = new ArrayList<>();
////		int bufSize = 1024 * 16;
////		if (bb.remaining() > bufSize) {
////			while (bb.hasRemaining()) {
////				ByteBuffer dup = bb.slice();
////				dup.limit(Math.min(bb.remaining(), bufSize));
////				bb.position(bb.position() + Math.min(bb.remaining(), bufSize));
////				list.add(dup);
////
////			}
////		} else {
////			list.add(bb);
////		}
////		PacketCombiner pc = new PacketCombiner();
////		long now = System.nanoTime();
////		List<UnfinishedPacket> o = null;
////		for (int i = 0; i < 100_000; i++)
////			for (ByteBuffer b : list) {
////				o = pc.put(b);
////				if (!o.isEmpty())
////					pc.bufferBuffer.unblockBuffer(o.get(0).buffer);
////			}
////		System.out.println(System.nanoTime() - now + " ns");
////		System.out.println((System.nanoTime() - now) / 100_000 + " ns/iter");
////		Packet rec = new Packet(o.get(0).getHeader());
////		rec.reconstruct(o.get(0).buffer.get());
//		Packet packet = new Packet(Packet.PING_PONG);
//		packet.setAwaitResponse(true);
//		packet.isClientTarget(false);
//		packet.setObject(new byte[1024 * 15]);
//		ByteBuffer bytes = packet.toBytes();
//		byte[] first = new byte[bytes.remaining() / 2];
//		bytes.get(first);
//		byte[] seconds = new byte[bytes.remaining()];
//		bytes.get(seconds);
//		PacketCombiner pc = new PacketCombiner(1024 * 128);
//		try {
//			System.out.println(pc.put(ByteBuffer.wrap(first)).size());
//			System.out.println(pc.put(ByteBuffer.wrap(seconds)).size());
//		} catch (Throwable th) {
//			th.printStackTrace();
//		}
//	}

	final int iid = id.getAndIncrement();
	private int arrPos = 4;
	private final byte[] sizeBytes = new byte[4];
	private DistributedByteBuffer buffer;
	
	private int size;

	private boolean hasSize = false;
	private final int maxSize;
	private int added = 0;
	private final ByteBufferDistributor bbd;

	public PacketCombiner(int maxSize, int cache) {
		this.maxSize = maxSize;
		this.bbd = new ByteBufferDistributor(maxSize, cache);
	}

	public synchronized List<UnfinishedPacket> put(ByteBuffer buf) {
		List<UnfinishedPacket> restored = new ArrayList<>();
		int p = buf.position();
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		buf.position(p);
		for (int pos = 0; pos < buf.limit();) {
			if (!hasSize) {
				sizeBytes[added] = buf.get(pos);
				added++;
				if (added == 4) {
					size = SerializationUtil.bytesToInt(sizeBytes);
					if (size <= 0 || size > maxSize)
						throw new IllegalArgumentException("invalid size: " + size + " " + ConversionUtil.bytesToHex(sizeBytes));
					hasSize = true;
					buffer = bbd.getBuffer();
					if(buffer == null) {
						buffer = new DistributedByteBuffer(ByteBuffer.allocate(size), -1);
					}
					buffer.buffer.limit(size);
					added = 0;
					buffer.buffer.put(sizeBytes);
				}
				pos++;
				continue;
			}
			int canAdd = buffer.buffer.limit() - buffer.buffer.position();
			canAdd = Math.min(size - arrPos, canAdd);
			canAdd = Math.min(buf.limit() - pos, canAdd);
			int willAdd = canAdd;
			buf.position(pos);
			int oldLimit = buf.limit();
			buf.limit(pos + willAdd);
			buffer.buffer.put(buf);
			buf.limit(oldLimit);
			arrPos += willAdd;
			pos += willAdd;

			if (arrPos == size) {
				UnfinishedPacket up = new UnfinishedPacket(SerializationUtil.bytesToShort(new byte[] { buffer.buffer.get(4), buffer.buffer.get(5) }));
				buffer.buffer.position(0);
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

	public void freeBuffer(DistributedByteBuffer dbb) {
		bbd.freeBuffer(dbb);
	}
}
