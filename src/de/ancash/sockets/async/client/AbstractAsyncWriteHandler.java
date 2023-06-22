package de.ancash.sockets.async.client;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;

import de.ancash.disruptor.MultiConsumerDisruptor;

public abstract class AbstractAsyncWriteHandler implements CompletionHandler<Integer, ByteBuffer> {

	private static long maxRateLimitNs = 250_000;
	private static long minRateLimitNs = 10_000;
	private static double growthConstant = -0.00000005;

	public static long getMaxRateLimit() {
		return maxRateLimitNs;
	}

	public static void setGrowthConstants(double d) {
		growthConstant = d;
	}

	public static double getGrowthConstant() {
		return growthConstant;
	}

	public static long getMinRateLimit() {
		return minRateLimitNs;
	}

	public static void setMaxRateLimit(long ns) {
		maxRateLimitNs = ns;
	}

	public static void setMinRateLimit(long ns) {
		minRateLimitNs = ns;
	}

	public static int calcRateLimit(int bps) {
		return (int) (maxRateLimitNs - (maxRateLimitNs - minRateLimitNs) * Math.pow(Math.E, growthConstant * bps));
	}

	private static MultiConsumerDisruptor<CheckWriteEvent> mc = new MultiConsumerDisruptor<>(CheckWriteEvent::new, 1024,
			ProducerType.MULTI, new SleepingWaitStrategy(0, 1), IntStream.range(0, 3).boxed()
					.map(i -> new CheckWriteEventHandler()).toArray(CheckWriteEventHandler[]::new));

	private static class CheckWriteEventHandler implements EventHandler<CheckWriteEvent> {

		@Override
		public void onEvent(CheckWriteEvent event, long sequence, boolean endOfBatch) throws Exception {
			while (System.nanoTime() - event.client.writeHandler.lastWrite < event.client.rateLimit) {
				LockSupport.parkNanos(100);
			}
			event.client.writeHandler.canWrite.set(true);
			event.client.checkWrite();
		}

	}

	protected final AbstractAsyncClient client;
	private AtomicBoolean canWrite = new AtomicBoolean(true);
	protected long lastWrite;

	public AbstractAsyncWriteHandler(AbstractAsyncClient asyncSocket) {
		this.client = asyncSocket;
	}

	@Override
	public void completed(Integer written, ByteBuffer bb) {
		if (written == -1 || !client.isConnectionValid()) {
			failed(new ClosedChannelException(), bb);
			return;
		}
		if (bb.hasRemaining()) {
			client.getAsyncSocketChannel().write(bb, bb, this);
			return;
		} else {
			lastWrite = System.nanoTime();
			mc.publishEvent((e, seq) -> {
				e.client = client;
			});
		}
	}

	@Override
	public void failed(Throwable arg0, ByteBuffer arg1) {
		client.setConnected(false);
		client.onDisconnect(arg0);
	}

	public boolean canWrite() {
		return canWrite.get();
	}

	public boolean write(ByteBuffer bb) {
		if (!canWrite.compareAndSet(true, false))
			return false;
		client.getAsyncSocketChannel().write(bb, bb, this);
		return true;
	}
}

class CheckWriteEvent {
	AbstractAsyncClient client;
}