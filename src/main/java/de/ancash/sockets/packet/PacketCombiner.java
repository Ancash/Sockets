package de.ancash.sockets.packet;

import static de.ancash.misc.ConversionUtil.*;

import java.util.ArrayList;
import java.util.List;

public class PacketCombiner {
	
	private byte[] allBytes;
	private int arrPos = 4;
	private byte[] sizeBytes = new byte[4];
	
	private int size;
	
	private boolean hasSize = false;
	
	private int added = 0;
		
	public synchronized List<UnfinishedPacket> put(byte...bytes) {
		List<UnfinishedPacket> restored = new ArrayList<>();
		for(int pos = 0; pos<bytes.length;) {
			
			if(!hasSize) {
				sizeBytes[added] = bytes[pos];
				added++;
								
				if(added == 4) {
					size = bytesToInt(sizeBytes);
					hasSize = true;
					allBytes = new byte[size];
					added = 0;
					System.arraycopy(sizeBytes, 0, allBytes, 0, 4);
				}
				pos++;
				continue;
			}
			
			int canAdd = allBytes.length - arrPos;
			int maxCanAdd = bytes.length - pos;
			int willAdd = canAdd > maxCanAdd ? maxCanAdd : canAdd;
			
			System.arraycopy(bytes, pos, allBytes, arrPos, willAdd);
			arrPos += willAdd;
			pos += willAdd;
			
			if(arrPos == size) {
				restored.add(new UnfinishedPacket().setHeader(bytesToShort(allBytes[4], allBytes[5])).setBytes(allBytes));
				hasSize = false;
				arrPos = 4;
				allBytes = null;
			}
		}
		return restored;
	}
	
	public byte[] getBytes() {
		return allBytes;
	}
}
