package de.ancash.sockets.io;

public enum ByteSizeConstants {

	_1, _2, _4, _8, _16, _32, _64, _128, _256, _512, _1024, _1024x2, _1024x4, _1024x8, _1024x16, _1024x32, _1024x64, _1024x128, _1024x256, _1024x512,
	_1024x1024;

	private final int size;

	private ByteSizeConstants() {
		size = (int) Math.pow(2, ordinal());
	}

	public int getSize() {
		return size;
	}

	public static ByteSizeConstants closest(int size) {
		ByteSizeConstants bsc = ByteSizeConstants._1;
		while (bsc.getSize() < size)
			bsc = ByteSizeConstants.values()[bsc.ordinal() + 1];
		return bsc;
	}
}
