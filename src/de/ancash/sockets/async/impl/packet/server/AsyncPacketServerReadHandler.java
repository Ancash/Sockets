package de.ancash.sockets.async.impl.packet.server;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;

import de.ancash.disruptor.SingleConsumerDisruptor;
import de.ancash.sockets.async.ByteEvent;
import de.ancash.sockets.async.ByteEventHandler;
import de.ancash.sockets.async.client.AbstractAsyncReadHandler;

public class AsyncPacketServerReadHandler extends AbstractAsyncReadHandler
		implements ByteEventHandler, EventHandler<ByteEvent> {

	protected final SingleConsumerDisruptor<ByteEvent> scd = new SingleConsumerDisruptor<ByteEvent>(ByteEvent::new,
			1024, ProducerType.SINGLE, new SleepingWaitStrategy(0, 1), this);

	public AsyncPacketServerReadHandler(AsyncPacketServer server, AsyncPacketServerClient asyncClient,
			int readBufSize) {
		super(asyncClient, readBufSize, null);
		super.byteHandler = this;
	}

	@Override
	public void onBytes(byte[] arr) {
		scd.publishEvent((e, seq) -> e.bytes = arr);
	}

	@Override
	public void onEvent(ByteEvent event, long sequence, boolean endOfBatch) throws Exception {
		client.onBytesReceive(event.bytes);
	}
}