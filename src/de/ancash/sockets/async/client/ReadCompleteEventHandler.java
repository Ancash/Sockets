package de.ancash.sockets.async.client;

import com.lmax.disruptor.EventHandler;

public class ReadCompleteEventHandler implements EventHandler<ReadCompleteEvent> {

	@Override
	public void onEvent(ReadCompleteEvent event, long sequence, boolean endOfBatch) {
		event.client.readHandler.complete(event.last, event.cnt);
	}
}
