package de.ancash.sockets.netty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCombiner;
import de.ancash.sockets.packet.UnfinishedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ReadHandler extends ChannelInboundHandlerAdapter {
	
	private static final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	private final PacketCombiner combiner = new PacketCombiner(1024 * 1024 * 2, 4);

	private final Consumer<Packet> packetHandler;
	private final Runnable onDisconnect;
	
	public ReadHandler(Consumer<Packet> packetHandler, Runnable onDisconnect) {
		this.packetHandler = packetHandler;
		this.onDisconnect = onDisconnect;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf byteBuf = (ByteBuf) msg;
		for(UnfinishedPacket packet : combiner.put(byteBuf.nioBuffer())) {
			pool.submit(() -> handleUnfinished(packet));
		}
		byteBuf.release();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext arg0) throws Exception {
		onDisconnect.run();
	}

	private void handleUnfinished(UnfinishedPacket up) {
		ByteBuffer buffer = up.buffer.buffer;
		Packet p = new Packet(up.getHeader());
		try {
			p.reconstruct(buffer);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		combiner.freeBuffer(up.buffer);
		packetHandler.accept(p);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}