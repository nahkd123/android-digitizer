package me.nahkd.adigitizer.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import me.nahkd.adigitizer.api.internal.GetEventPatterns;
import me.nahkd.adigitizer.api.prop.EventProperty;
import me.nahkd.adigitizer.api.prop.KeyPropertyInfo;
import me.nahkd.adigitizer.api.prop.PropertyInfo;
import me.nahkd.adigitizer.api.prop.RangePropertyInfo;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

class AndroidDeviceImpl implements AndroidDevice {
	private JadbDevice adbDevice;
	private List<InputDeviceImpl> inputDevices;

	private AndroidDeviceImpl(JadbDevice adbDevice, List<InputDeviceImpl> inputDevices) {
		this.adbDevice = adbDevice;
		this.inputDevices = inputDevices;
	}

	public static AndroidDeviceImpl createFrom(JadbDevice adbDevice) throws IOException, JadbException {
		List<InputDeviceImpl> inputDevices = new ArrayList<>();

		// Scanning
		String currentDevice = null;
		String currentSection = null;
		int currentType = -1;
		Map<String, Object> properties = null;
		List<PropertyInfo> eventProps = null;
		Set<String> inputProps = null;

		try (Scanner scanner = new Scanner(adbDevice.execute("getevent", "-i"))) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				Matcher m;

				if ((m = GetEventPatterns.L0_ADD_DEVICE.matcher(line)).matches()) {
					if (currentDevice != null) {
						String name = properties.getOrDefault("name", currentDevice).toString();
						InputDeviceImpl dev = new InputDeviceImpl(adbDevice, currentDevice, name, eventProps);
						inputDevices.add(dev);
					}

					currentDevice = m.group("device");
					properties = new HashMap<>();
					inputProps = new HashSet<>();
					eventProps = new ArrayList<>();
					continue;
				}

				if ((m = GetEventPatterns.L1_PROPERTY.matcher(line)).matches()) {
					String key = m.group("name");
					String valueInt = m.group("integer");
					String valueStr = m.group("string");
					String valueVer = m.group("version");
					properties.put(key, valueInt != null ? valueInt : valueStr != null ? valueStr : valueVer);
					continue;
				}

				if ((m = GetEventPatterns.L1_SECTION.matcher(line)).matches()) {
					currentSection = m.group("name");
					continue;
				}

				if ("input props".equals(currentSection)
					&& (m = GetEventPatterns.L2_INPUT_PROPS.matcher(line)).matches()) {
					String val = m.group("value");
					if (val != null) inputProps.add(val);
					continue;
				}

				if ("events".equals(currentSection) && (m = GetEventPatterns.L2_EVENTS.matcher(line)).matches()) {
					String type = m.group("type");
					String code = m.group("code");
					int[] codes = m.group("enum") != null
						? Stream.of(m.group("enum").split(" +")).mapToInt(s -> Integer.parseInt(s, 16)).toArray()
						: null;
					String min = m.group("min");
					String max = m.group("max");
					String fuzz = m.group("fuzz");

					if (type != null) currentType = Integer.parseInt(type, 16);

					if (code != null) {
						int ecode = Integer.parseInt(code, 16);
						int iMin = Integer.parseInt(min);
						int iMax = Integer.parseInt(max);
						int iFuzz = Integer.parseInt(fuzz);
						EventProperty prop = EventProperty.of((short) currentType, (short) ecode);
						RangePropertyInfo range = new RangePropertyInfo(prop, iMin, iMax, iFuzz);
						eventProps.add(range);
					}

					if (codes != null) {
						for (int kcode : codes) {
							EventProperty prop = EventProperty.of((short) currentType, (short) kcode);
							KeyPropertyInfo key = new KeyPropertyInfo(prop);
							eventProps.add(key);
						}
					}

					continue;
				}

				System.out.println("Unable to parse '" + line + "'");
			}
		}

		return new AndroidDeviceImpl(adbDevice, inputDevices);
	}

	@Override
	public JadbDevice getAdbDevice() { return adbDevice; }

	@Override
	public List<? extends InputDevice> getInputDevices() { return Collections.unmodifiableList(inputDevices); }
}
