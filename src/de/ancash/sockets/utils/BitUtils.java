package de.ancash.sockets.utils;

public class BitUtils {

	public static byte setBit(byte b, int pos) {
		return (byte) (b | (1 << pos));
	}
	
	public static byte unsetBit(byte b, int pos) {
		return (byte) (b & ~(1 << pos));
	}
}