package me.nahkd.adigitizer.api.internal;

import java.io.IOException;
import java.io.InputStream;

public final class StreamHelper {
	public static long readLongLE(InputStream s) throws IOException {
		byte[] bs = s.readNBytes(8);
		long l = 0L;
		for (int i = 0; i < 8; i++) l |= (bs[i] & 0xFFL) << (i * 8);
		return l;
	}

	public static int readIntLE(InputStream s) throws IOException {
		byte[] bs = s.readNBytes(4);
		int intVal = 0;
		for (int i = 0; i < 4; i++) intVal |= (bs[i] & 0xFF) << (i * 8);
		return intVal;
	}

	public static short readShortLE(InputStream s) throws IOException {
		byte[] bs = s.readNBytes(2);
		return (short) ((bs[0] & 0xFF) | ((bs[1] & 0xFF) << 8));
	}
}
