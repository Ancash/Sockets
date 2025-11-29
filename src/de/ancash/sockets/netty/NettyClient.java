package de.ancash.sockets.netty;

import java.util.function.Consumer;

import de.ancash.sockets.packet.Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {

	public static void main(String[] args) throws InterruptedException {
		NettyClient cl = new NettyClient("localhost", 25000, null);
		cl.setOnConnect(() -> System.out.println("cn"));
		cl.setOnDisconnect(() -> System.out.println("dc"));
		cl.connect();
		
	}
	
	private final String host;
	private final int port;
	private SocketChannel channel;
	private final Consumer<Packet> packetHandler;
	private Runnable onConnect;
	private Runnable onDisconnect;
	
	public NettyClient(String host, int port, Consumer<Packet> packetHandler) {
		this.port = port;
		this.packetHandler = packetHandler;
		this.host = host;
	}
	
	public void setOnDisconnect(Runnable onDisconnect) {
		this.onDisconnect = onDisconnect;
	}
	
	public void setOnConnect(Runnable onConnect) {
		this.onConnect = onConnect;
	}
	
	public synchronized void disconnect() {
		if(channel != null) {
			channel.close();
			channel = null;
		}
	}
	
	public synchronized boolean connect() throws InterruptedException {
		if(channel != null) {
			channel.close();
			channel = null;
		}
		Bootstrap b = new Bootstrap(); // (1)
		
        b.group(new NioEventLoopGroup()); // (2)
        b.channel(NioSocketChannel.class); // (3)
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            
        	@Override
            public void initChannel(SocketChannel ch) throws Exception {
            	channel = ch;
                ch.pipeline().addLast(new ReadHandler(packetHandler, onDisconnect));
            }
        });
        b.connect(host, port).await(5_000); // (5)
        if(isConnected())
        	onConnect.run();
        return isConnected();
	}
	
	public void write(Packet packet) {
		ByteBuf buf = Unpooled.wrappedBuffer(packet.toBytes());
		channel.writeAndFlush(buf);
	}
	
	public boolean isConnected() {
		return channel != null && channel.isActive();
	}
}
