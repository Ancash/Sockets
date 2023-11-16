package de.ancash.sockets.packet;

public class SerializationUtil {
	
	public static byte[] stringToBytes(String s) {
		char[] chars = s.toCharArray();
		byte[] b = new byte[chars.length * 2];
		int bp = 0;
		for(char c : chars) {
			byte[] bc = charToBytes(c);
			b[bp] = bc[0];
			bp++;
			b[bp] = bc[1];
			bp++;
		}
		return b;
	}
	
	public static String bytesToString(byte[] b) {
		char[] c = new char[b.length / 2];
		for(int i = 0; i<c.length;) {
			c[i] = bytesToChar(new byte[] {b[i * 2], b[i * 2 + 1]});
			i++;
		}
		return String.valueOf(c);
	}
	
	public static byte[] doubleToBytes(double d) {
		return longToBytes(Double.doubleToLongBits(d));
	}

	public static double bytesToDouble(byte[] b) {
		return Double.longBitsToDouble(bytesToLong(b));
	}
	
	public static byte[] floatToBytes(float d) {
		return intToBytes(Float.floatToIntBits(d));
	}

	public static float floatToDouble(byte[] b) {
		return Float.intBitsToFloat(bytesToInt(b));
	}

	public static byte[] longToBytes(long l) {
		byte[] result = new byte[Long.BYTES];
		for (int i = Long.BYTES - 1; i >= 0; i--) {
			result[i] = (byte) (l & 0xFF);
			l >>= Byte.SIZE;
		}
		return result;
	}

	public static long bytesToLong(final byte[] b) {
		long result = 0;
		for (int i = 0; i < Long.BYTES; i++) {
			result <<= Byte.SIZE;
			result |= (b[i] & 0xFF);
		}
		return result;
	}

	public static byte[] intToBytes(int i) {
		return new byte[] { (byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i };
	}

	public static int bytesToInt(byte[] b) {
		return (b[0]) << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF);
	}

	public static byte[] shortToBytes(short s) {
		return new byte[] { (byte) s, (byte) ((s >> 8) & 0xff) };
	}

	public static short bytesToShort(byte[] b) {
		return (short) ((b[1] << 8) + (b[0] & 0xFF));
	}

	public static byte[] charToBytes(char s) {
		return new byte[] { (byte) s, (byte) ((s >> 8) & 0xff) };
	}

	public static char bytesToChar(byte[] b) {
		return (char) ((b[1] << 8) + (b[0] & 0xFF));
	}
}
