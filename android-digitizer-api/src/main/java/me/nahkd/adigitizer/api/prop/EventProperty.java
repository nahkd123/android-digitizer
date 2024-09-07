package me.nahkd.adigitizer.api.prop;

import java.util.HashMap;
import java.util.Map;

public interface EventProperty {
	/**
	 * <p>
	 * Get programmer-friendly name of this event property. The name is guaranteed
	 * to only contains the characters in {@code [A-Za-z0-9/_-]} set.
	 * </p>
	 */
	String getName();

	Map<Integer, EventProperty> INTERNAL_MAP = new HashMap<>();
	Map<Short, String> TYPES = Map.ofEntries(
		Map.entry((short) 0x00, "syn"),
		Map.entry((short) 0x01, "key"),
		Map.entry((short) 0x02, "rel"),
		Map.entry((short) 0x03, "abs"),
		Map.entry((short) 0x04, "msc"),
		Map.entry((short) 0x05, "sw"),
		Map.entry((short) 0x11, "led"),
		Map.entry((short) 0x12, "snd"),
		Map.entry((short) 0x14, "rep"),
		Map.entry((short) 0x15, "ff"),
		Map.entry((short) 0x17, "pwr"));

	static EventProperty internalOf(int type, int code, String name) {
		return INTERNAL_MAP.computeIfAbsent((type << 16) | code, $ -> new EventPropertyImpl(type, code, name));
	}

	static EventProperty of(short type, short code) {
		return internalOf(
			type & 0xFFFF,
			code & 0xFFFF,
			"%s/%04X".formatted(TYPES.getOrDefault(type, "%04X".formatted(type)), code));
	}

	EventProperty SYN_REPORT = internalOf(0x00, 0, "syn/report");
	EventProperty SYN_CONFIG = internalOf(0x00, 1, "syn/config");
	EventProperty SYN_MT_REPORT = internalOf(0x00, 2, "syn/mt_report");
	EventProperty SYN_DROPPED = internalOf(0x00, 3, "syn/dropped");

	EventProperty KEY_VOLUP = internalOf(0x01, 114, "key/volup");
	EventProperty KEY_VOLDOWN = internalOf(0x01, 115, "key/voldown");
	EventProperty KEY_POWER = internalOf(0x01, 116, "key/power");
	EventProperty KEY_PEN = internalOf(0x01, 0x0140, "key/pen");
	EventProperty KEY_ERASER = internalOf(0x01, 0x0141, "key/eraser");
	EventProperty KEY_FINGER = internalOf(0x01, 0x0145, "key/finger");
	EventProperty KEY_TOUCH = internalOf(0x01, 0x014A, "key/touch");
	EventProperty KEY_BTN_STYLUS = internalOf(0x01, 0x014B, "key/btn_stylus");
	EventProperty KEY_BTN_STYLUS2 = internalOf(0x01, 0x014C, "key/btn_stylus2");

	EventProperty ABS_X = internalOf(0x03, 0x00, "abs/x");
	EventProperty ABS_Y = internalOf(0x03, 0x01, "abs/y");
	EventProperty ABS_Z = internalOf(0x03, 0x02, "abs/z");
	EventProperty ABS_RX = internalOf(0x03, 0x03, "abs/rx");
	EventProperty ABS_RY = internalOf(0x03, 0x04, "abs/ry");
	EventProperty ABS_RZ = internalOf(0x03, 0x05, "abs/rz");
	EventProperty ABS_PRESSURE = internalOf(0x03, 0x18, "abs/pressure");
	EventProperty ABS_DISTANCE = internalOf(0x03, 0x19, "abs/distance");
	EventProperty ABS_TILT_X = internalOf(0x03, 0x1A, "abs/tilt_x");
	EventProperty ABS_TILT_Y = internalOf(0x03, 0x1B, "abs/tilt_y");
	EventProperty ABS_TOOL_WIDTH = internalOf(0x03, 0x1B, "abs/tool_width");
	EventProperty ABS_MT_TOUCH_MAJOR = internalOf(0x03, 0x30, "abs/mt_touch_major");
	EventProperty ABS_MT_TOUCH_MINOR = internalOf(0x03, 0x31, "abs/mt_touch_major");
	EventProperty ABS_MT_TRACKING_ID = internalOf(0x03, 0x39, "abs/mt_tracking_id");
	EventProperty ABS_MT_POSITION_X = internalOf(0x03, 0x35, "abs/mt_position_x");
	EventProperty ABS_MT_POSITION_Y = internalOf(0x03, 0x36, "abs/mt_position_y");
}
