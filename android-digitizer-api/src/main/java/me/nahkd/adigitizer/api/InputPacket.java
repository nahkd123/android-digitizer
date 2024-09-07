package me.nahkd.adigitizer.api;

import java.io.IOException;
import java.io.InputStream;

// input_event { timeval timestamp; short type; short code; int value; }
// timeval { time_t seconds; suseconds_t microseconds; }
// time_t long
// suseconds_t long

/**
 * <p>
 * Modelled based on {@code linux/input.h}.
 * </p>
 */
public record InputPacket(long seconds, long microseconds, short type, short code, int value) {
	public static InputPacket readFromStream(InputStream stream) throws IOException {
		long seconds = readLong(stream);
		long microseconds = readLong(stream);
		short type = (short) (stream.read() | (stream.read() << 8));
		short code = (short) (stream.read() | (stream.read() << 8));
		int value = (stream.read()) | (stream.read() << 8) | (stream.read() << 16) | (stream.read() << 24);
		return new InputPacket(seconds, microseconds, type, code, value);
	}

	private static long readLong(InputStream stream) throws IOException {
		byte[] bs = stream.readNBytes(8);
		long l = 0L;
		for (int i = 0; i < 8; i++) l |= (bs[i] & 0xFFL) << (i * 8);
		return l;
	}

	public static final short EV_SYN = 0;
	public static final short SYN_REPORT = 0;
	public static final short SYN_CONFIG = 1;
	public static final short SYN_MT_REPORT = 2;
	public static final short SYN_DROPPED = 3;

	public static final short EV_KEY = 0x01;

	public static final short EV_REL = 0x02;
	public static final short REL_X = 0x00;
	public static final short REL_Y = 0x01;
	public static final short REL_Z = 0x02;
	public static final short REL_RX = 0x03;
	public static final short REL_RY = 0x04;
	public static final short REL_RZ = 0x05;
	public static final short REL_HWHEEL = 0x06;
	public static final short REL_DIAL = 0x07;
	public static final short REL_WHEEL = 0x08;
	public static final short REL_MISC = 0x09;
	public static final short REL_RESERVED = 0x0A;
	public static final short REL_WHEEL_HI_RES = 0x0B;
	public static final short REL_HWHEEL_HI_RES = 0x0C;

	public static final short EV_ABS = 0x03;
	public static final short ABS_X = 0x00;
	public static final short ABS_Y = 0x01;
	public static final short ABS_Z = 0x02;
	public static final short ABS_RX = 0x03;
	public static final short ABS_RY = 0x04;
	public static final short ABS_RZ = 0x05;
	public static final short ABS_THROTTLE = 0x05;
	public static final short ABS_RUDDER = 0x05;
	public static final short ABS_WHEEL = 0x05;
	public static final short ABS_GAS = 0x05;
	public static final short ABS_BRAKE = 0x05;
	public static final short ABS_HAT0X = 0x05;
	public static final short ABS_HAT0Y = 0x05;
	public static final short ABS_HAT1X = 0x05;
	public static final short ABS_HAT1Y = 0x05;
	public static final short ABS_HAT2X = 0x05;
	public static final short ABS_HAT2Y = 0x05;
	public static final short ABS_HAT3X = 0x05;
	public static final short ABS_HAT3Y = 0x05;
	public static final short ABS_PRESSURE = 0x05;
	public static final short ABS_DISTANCE = 0x05;
	public static final short ABS_TILT_X = 0x05;
	public static final short ABS_TILT_Y = 0x05;
	public static final short ABS_TOOl_WIDTH = 0x05;
	public static final short ABS_VOLUME = 0x05;
	public static final short ABS_PROFILE = 0x05;
	public static final short ABS_MISC = 0x05;

	public static final short EV_MSC = 0x04;
	public static final short EV_SW = 0x05;
	public static final short EV_LED = 0x11;
	public static final short EV_SND = 0x12;
	public static final short EV_REP = 0x14;
	public static final short EV_FF = 0x15;
	public static final short EV_PWR = 0x16;

	@Override
	public final String toString() {
		return "%010d.%06d [%04X.%04X] %08X".formatted(seconds(), microseconds(), type, code, value);
	}
}
