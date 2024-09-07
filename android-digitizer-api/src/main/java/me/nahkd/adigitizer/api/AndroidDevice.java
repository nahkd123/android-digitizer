package me.nahkd.adigitizer.api;

import java.io.IOException;
import java.util.List;

import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

public interface AndroidDevice {
	JadbDevice getAdbDevice();

	List<? extends InputDevice> getInputDevices();

	static AndroidDevice createFrom(JadbDevice adbDevice) throws IOException, JadbException {
		return AndroidDeviceImpl.createFrom(adbDevice);
	}
}
