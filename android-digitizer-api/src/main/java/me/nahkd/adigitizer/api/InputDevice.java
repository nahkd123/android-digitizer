package me.nahkd.adigitizer.api;

import java.util.Map;
import java.util.function.Consumer;

import me.nahkd.adigitizer.api.prop.EventProperty;
import me.nahkd.adigitizer.api.prop.PropertyInfo;

public interface InputDevice {
	String getName();

	Map<EventProperty, PropertyInfo> getProperties();

	boolean isPollingEvents();

	void startPolling();

	void stopPolling();

	void addListener(Consumer<InputEvent> listener);

	void removeListener(Consumer<InputEvent> listener);
}
