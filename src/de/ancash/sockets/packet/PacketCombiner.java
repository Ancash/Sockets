package de.ancash.sockets.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.ancash.Sockets;
import de.ancash.misc.ConversionUtil;
import de.ancash.sockets.io.BigByteBuffer;
import de.ancash.sockets.io.ByteBufferDistributor;
import de.ancash.sockets.io.ByteSizeConstants;
import de.ancash.sockets.io.PositionedByteBuf;

public class PacketCombiner {

//	private static final BigByteBuffer bbb = new BigByteBuffer(ByteSizeConstants._128, ByteSizeConstants._1024x256, 8);

	static AtomicInteger id = new AtomicInteger();

	static byte get(ByteBuffer bb, int index) {
		return 0;
	}

	public static void main(String[] args)
			throws ClassNotFoundException, NoSuchFieldException, SecurityException, IOException {
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
		PacketCombiner pc = new PacketCombiner(1024 * 128, 1024);
		try {
			System.out.println(pc.put(o).size());
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}

	final int iid = id.getAndIncrement();
	private int arrPos = 4;
	private byte[] sizeBytes = new byte[4];
	private final ByteBufferDistributor bufferBuffer;
	private PositionedByteBuf buffer;

	private int size;

	private boolean hasSize = false;
	private final int maxSize;
	private int added = 0;

	public PacketCombiner(int maxSize, int bufSize) {
		this(maxSize, bufSize, bufSize / 14 + 1);
	}

	public PacketCombiner(int maxSize, int bufSize, int buffers) {
		bufferBuffer = new ByteBufferDistributor(maxSize, buffers);
		this.maxSize = maxSize;
	}

	@SuppressWarnings("nls")
	public synchronized List<UnfinishedPacket> put(ByteBuffer buf) {
		int i = 0;
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
					long l = System.currentTimeMillis() + 1000;
//					buffer = bbb.blockBuffer(size);
					while ((buffer = bufferBuffer.getAvailableBuffer()) == null) {
						Sockets.sleep(10_000);
						if (l < System.currentTimeMillis()) {
							System.err.println(Thread.currentThread().getName()
									+ "no free buffer available/all used up in current invokation. reconstruced " + i
									+ " packets cap " + bufferBuffer.capacity());
							l = System.currentTimeMillis() + 1000;
							for (PositionedByteBuf pbb : bufferBuffer.buffers)
								if (pbb.owner != null) {
//									System.out.println(pbb.owner.getName() + ":--------------");
//									for(StackTraceElement e : pbb.owner.getStackTrace())
//										System.out.println(e.getClassName() + ":" + e.getMethodName()  +":" + e.getLineNumber());
								}

////							buffer = new PositionedByteBuf(ByteBuffer.allocate(size), -1);
////							allocateNew = true;
////							break;
						}
					}
					buffer.get().limit(size);
					added = 0;
					buffer.get().put(sizeBytes);
				}
				pos++;
				continue;
			}
			int canAdd = buffer.get().limit() - buffer.get().position();
			canAdd = Math.min(size - arrPos, canAdd);
			canAdd = Math.min(buf.limit() - pos, canAdd);
			int willAdd = canAdd;
			buf.position(pos);
			int oldLimit = buf.limit();
			buf.limit(pos + willAdd);
			buffer.get().put(buf);
			buf.limit(oldLimit);
			arrPos += willAdd;
			pos += willAdd;

			if (arrPos == size) {
				UnfinishedPacket up = new UnfinishedPacket().setHeader(
						SerializationUtil.bytesToShort(new byte[] { buffer.get().get(4), buffer.get().get(5) }));
				buffer.get().position(0);
				buffer.owner = Thread.currentThread();
				up.buffer = buffer;
				buffer = null;
				restored.add(up);
				hasSize = false;
				arrPos = 4;
				i++;
			}
		}
		return restored;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void unblockBuffer(PositionedByteBuf buffer2) {
//		bbb.unblockBuffer(buffer2);
		bufferBuffer.unblockBuffer(buffer2);
	}
}
