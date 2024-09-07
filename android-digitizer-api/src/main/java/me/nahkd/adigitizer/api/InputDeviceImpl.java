package me.nahkd.adigitizer.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.nahkd.adigitizer.api.internal.StreamHelper;
import me.nahkd.adigitizer.api.prop.EventProperty;
import me.nahkd.adigitizer.api.prop.PropertyInfo;
import se.vidstige.jadb.JadbDevice;

class InputDeviceImpl implements InputDevice {
	private JadbDevice androidDevice;
	private String path;
	private String name;
	private Map<EventProperty, PropertyInfo> properties;

	// Polling
	private Thread pollingThread = null;
	private List<PropertyInfo> orderedInfo;
	Map<EventProperty, Integer> propertyToPacketPos;

	// Listening
	Set<Consumer<InputEvent>> listeners = new HashSet<>();
	Set<Consumer<InputEvent>> pendingAddListeners = new HashSet<>();
	Set<Consumer<InputEvent>> pendingRemoveListeners = new HashSet<>();
	boolean emittingEvent = false;

	public InputDeviceImpl(JadbDevice androidDevice, String path, String name, List<PropertyInfo> properties) {
		this.androidDevice = androidDevice;
		this.path = path;
		this.name = name;
		this.properties = properties.stream().collect(Collectors.toMap(p -> p.property(), p -> p));
	}

	@Override
	public String getName() { return name; }

	@Override
	public Map<EventProperty, PropertyInfo> getProperties() { return properties; }

	@Override
	public boolean isPollingEvents() { return pollingThread != null; }

	@Override
	public void startPolling() {
		if (pollingThread != null) return;

		if (orderedInfo == null) {
			orderedInfo = new ArrayList<>(properties.values());
			propertyToPacketPos = new HashMap<>();
			for (int i = 0; i < orderedInfo.size(); i++) propertyToPacketPos.put(orderedInfo.get(i).property(), i);
		}

		pollingThread = new Thread(() -> {
			try {
				InputStream stream = androidDevice.execute("cat", path);
				long tsSec, tsMicro;
				int[] packetData = new int[orderedInfo.size()];

				while (!Thread.interrupted()) {
					while (stream.available() > 0) {
						tsSec = StreamHelper.readLongLE(stream);
						tsMicro = StreamHelper.readLongLE(stream);
						short type = StreamHelper.readShortLE(stream);
						short code = StreamHelper.readShortLE(stream);
						int value = StreamHelper.readIntLE(stream);
						EventProperty prop = EventProperty.of(type, code);

						if (prop == EventProperty.SYN_REPORT) {
							// Submit
							int[] dataClone = packetData.clone();
							InputEventImpl event = new InputEventImpl(this, tsSec, tsMicro, dataClone);

							emittingEvent = true;
							for (Consumer<InputEvent> l : listeners) l.accept(event);
							emittingEvent = false;
							for (Consumer<InputEvent> l : pendingAddListeners) {
								listeners.add(l);
								l.accept(event);
							}
							for (Consumer<InputEvent> l : pendingRemoveListeners) listeners.remove(l);
							pendingAddListeners.clear();
							pendingRemoveListeners.clear();
						} else {
							int i = propertyToPacketPos.getOrDefault(prop, -1);
							if (i == -1) continue;
							packetData[i] = value;
						}
					}

					try {
						Thread.sleep(1L);
					} catch (InterruptedException e) {
						break;
					}
				}
			} catch (Throwable e) {
				throw new RuntimeException(e);
			} finally {
				emittingEvent = false;
			}
		});
		pollingThread.start();
	}

	@Override
	public void stopPolling() {
		if (pollingThread == null) return;
		pollingThread.interrupt();
		pollingThread = null;
	}

	@Override
	public void addListener(Consumer<InputEvent> listener) {
		if (!emittingEvent) listeners.add(listener);
		else pendingAddListeners.add(listener);
	}

	@Override
	public void removeListener(Consumer<InputEvent> listener) {
		if (!emittingEvent) listeners.remove(listener);
		else pendingRemoveListeners.add(listener);
	}

	@Override
	public String toString() {
		return "device " + path + " (" + name + ")";
	}
}
