package me.nahkd.adigitizer.vmulti;

import java.io.IOException;

import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;

import io.github.nahkd123.vmulti4j.VMultiIO;
import io.github.nahkd123.vmulti4j.report.DigitzerButtons;
import me.nahkd.adigitizer.api.AndroidDevice;
import me.nahkd.adigitizer.api.InputDevice;
import me.nahkd.adigitizer.api.prop.EventProperty;
import me.nahkd.adigitizer.api.prop.RangePropertyInfo;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

public class SimpleMain {
	public static void main(String[] args) throws IOException, JadbException {
		HidServicesSpecification hidServicesSpec = new HidServicesSpecification();
		hidServicesSpec.setAutoStart(false);
		hidServicesSpec.setAutoDataRead(false);
		HidServices hidServices = HidManager.getHidServices(hidServicesSpec);
		hidServices.start();
		VMultiIO vmulti = VMultiIO.getIOStream(hidServices.getAttachedHidDevices()).thenOpen();

		if (vmulti == null) {
			System.err.println("Unable to obtain VMulti HID stream - Have you installed it yet?");
			System.exit(1);
		} else {
			System.out.println("Obtained VMulti stream!");
		}

		JadbConnection connection = new JadbConnection();
		JadbDevice device = connection.getAnyDevice(); // TODO - fix this with selector
		AndroidDevice android = AndroidDevice.createFrom(device);
		System.out.println("Established connection to Android device!");

		for (InputDevice input : android.getInputDevices()) {
			if (input.getProperties().get(EventProperty.ABS_MT_POSITION_X) instanceof RangePropertyInfo) {
				// TODO add touch
				break;
			} else if (input.getProperties().get(EventProperty.ABS_X) instanceof RangePropertyInfo rangeX) {
				if (!(input.getProperties().get(EventProperty.ABS_Y) instanceof RangePropertyInfo rangeY)) break;
				RangePropertyInfo pressure = input.getProperties()
					.get(EventProperty.ABS_PRESSURE) instanceof RangePropertyInfo p ? p : null;
				RangePropertyInfo tiltX = input.getProperties()
					.get(EventProperty.ABS_TILT_X) instanceof RangePropertyInfo p ? p : null;
				RangePropertyInfo tiltY = input.getProperties()
					.get(EventProperty.ABS_TILT_Y) instanceof RangePropertyInfo p ? p : null;

				input.addListener(event -> {
					double x = rangeX.mapToDouble(event.get(EventProperty.ABS_X));
					double y = rangeY.mapToDouble(event.get(EventProperty.ABS_Y));
					double p = pressure != null ? pressure.mapToDouble(event.get(EventProperty.ABS_PRESSURE)) : 0d;
					int tx = tiltX != null ? event.get(EventProperty.ABS_TILT_X) : 0;
					int ty = tiltY != null ? event.get(EventProperty.ABS_TILT_Y) : 0;
					vmulti.reportDigitizer(new DigitzerButtons(true, p > 0d, false, false, false), x, y, p, tx, ty);
				});

				input.startPolling();
				System.out.println("Attached absolute pointer listener to %s".formatted(input.getName()));
			}
		}
	}
}
