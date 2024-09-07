package me.nahkd.adigitizer.api;

import java.util.Collection;

import me.nahkd.adigitizer.api.prop.EventProperty;

record InputEventImpl(InputDeviceImpl device, long timestampSecond, long timestampMicrosecond, int[] data) implements InputEvent {
	@Override
	public Collection<EventProperty> getProperties() { return device.propertyToPacketPos.keySet(); }

	@Override
	public int get(EventProperty property) {
		int i = device.propertyToPacketPos.getOrDefault(property, -1);
		return i == -1 ? 0 : data[i];
	}

	@Override
	public final String toString() {
		return "%s @ %d.%06d".formatted(device.getName(), timestampSecond, timestampMicrosecond);
	}
}
