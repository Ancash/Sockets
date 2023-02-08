package de.ancash.sockets.packet;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PacketFuture {

	private final Packet packet;
	private final UUID uuid;
	
	public PacketFuture(Packet packet, UUID uuid) {
		this.packet = packet;
		this.uuid = uuid;
	}
	
	public boolean isValid() {
		return packet != null && uuid != null;
	}
	
	public boolean isDone() {
		return packet != null && packet.getResponse() != null;
	}
	
	public <T> Optional<T> get() {
		return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> get(long timeout, TimeUnit unit) {
		if(packet == null)
			return Optional.empty();
		try {
			Optional<Packet> optional = packet.awaitResponse(unit.toMillis(timeout));
			if(!optional.isPresent())
				return Optional.empty();
			return Optional.ofNullable((T) optional.get().getSerializable());
		} catch (InterruptedException e) {
			return Optional.empty();
		}
	}

	public Packet getPacket() {
		return packet;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public long getTimestamp() {
		return packet.getTimeStamp();
	}
}