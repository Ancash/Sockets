package de.ancash.sockets.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import de.ancash.sockets.io.IByteBuffer;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Proudly stolen from https://github.com/addthis/stream-lib
 */

public final class VarUtils {

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final int SEGMENT_BITS = 0x7F;
	private static final int CONTINUE_BIT = 0x80;

	private VarUtils() {
	}

	public static String readVarString(IByteBuffer in) {
		return readVarString(in, Integer.MAX_VALUE);
	}

	public static String readVarString(IByteBuffer in, int maxLen) {
		int size = readVarInt(in);
		byte[] bytes = new byte[size];
		for (int i = 0; i < size; i++)
			bytes[i] = in.pollFirst();
		return new String(bytes, Charset.forName("UTF-8"));
	}

	public static byte[] writeVarString(String str) {
		byte[] stringBytes = str.getBytes(UTF_8);
		int size = stringBytes.length;
		byte[] sizeBytes = writeVarInt(size);
		ByteArrayOutputStream out = new ByteArrayOutputStream(size + sizeBytes.length);
		try {
			out.write(sizeBytes);
			out.write(stringBytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	public static int readVarInt(IByteBuffer bb) {
		int value = 0;
		int position = 0;
		byte currentByte;

		while (true) {
			currentByte = bb.pollFirst();
			value |= (currentByte & SEGMENT_BITS) << position;

			if ((currentByte & CONTINUE_BIT) == 0)
				break;

			position += 7;

			if (position >= 32)
				throw new RuntimeException("VarInt is too big");
		}

		return value;
	}

	public static long readVarLong(IByteBuffer bb) {
		long value = 0;
		int position = 0;
		byte currentByte;

		while (true) {
			currentByte = bb.pollFirst();
			value |= (currentByte & SEGMENT_BITS) << position;

			if ((currentByte & CONTINUE_BIT) == 0)
				break;

			position += 7;

			if (position >= 64)
				throw new RuntimeException("VarLong is too big");
		}

		return value;
	}

	public static byte[] writeVarInt(int value) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (true) {
			if ((value & ~SEGMENT_BITS) == 0) {
				out.write(value);
				return out.toByteArray();
			}

			out.write((value & SEGMENT_BITS) | CONTINUE_BIT);
			// Note: >>> means that the sign bit is shifted with the rest of the number
			// rather than being left alone
			value >>>= 7;
		}
	}

	public static byte[] writeVarLong(long value) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (true) {
			if ((value & ~((long) SEGMENT_BITS)) == 0) {
				out.write((int) value);
				return out.toByteArray();
			}

			out.write((int) ((value & SEGMENT_BITS) | CONTINUE_BIT));
			// Note: >>> means that the sign bit is shifted with the rest of the number
			// rather than being left alone
			value >>>= 7;
		}
	}
}