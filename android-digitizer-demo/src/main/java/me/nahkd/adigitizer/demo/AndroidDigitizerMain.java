package me.nahkd.adigitizer.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiSliderFlags;
import imgui.type.ImBoolean;
import me.nahkd.adigitizer.api.AndroidDevice;
import me.nahkd.adigitizer.api.InputDevice;
import me.nahkd.adigitizer.api.InputEvent;
import me.nahkd.adigitizer.api.prop.EventProperty;
import me.nahkd.adigitizer.api.prop.PropertyInfo;
import me.nahkd.adigitizer.api.prop.RangePropertyInfo;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbException;

public class AndroidDigitizerMain extends Application {
	// This application read pen data from your Android phone through ADB. You can
	// use this to test the digitizer on your phone.
	//
	// Instructions:
	// 0. Make sure your Android device has pressure sensitive pen support (S-Pen,
	// USI, etc...)
	// 1. Plug your device with USB debugging to your computer
	// 2. Accept USB debugging prompt (if any)

	public static void main(String[] args) throws Throwable {
		launch(new AndroidDigitizerMain());
	}

	private JadbConnection adb;
	private List<AndroidDevice> androidDevices;
	private AndroidDevice selectedDevice = null;
	private InputDevice selectedInput = null;

	// ImGui
	private ImFont font;

	// Windows
	private ImBoolean selectDevicesWindow = new ImBoolean(true);
	private ImBoolean inspectorWindow = new ImBoolean(true);
	private ImBoolean visualInspectorWindow = new ImBoolean(true);

	// Inspection
	private InputEvent lastEvent = null;
	private BigDecimal calculatedRate = null;
	private List<BigDecimal> rateHistory = new ArrayList<>();
	private BigDecimal averageRate = null;
	private List<InputDeviceDataPoint> trail = new ArrayList<>();
	private int[] trailLength = new int[] { 100 };
	private int[] visualizeRadius = new int[] { 100 };
	private float[] visualizeExponent = new float[] { 1f };
	private boolean enableZ = true;
	private boolean enableTilt = true;
	private boolean enableFadingTrail = true;
	private FileSaveGui exportCsvPopup = new FileSaveGui(new File("."), "visual_inspector.csv");

	public AndroidDigitizerMain() throws Throwable {
		adb = new JadbConnection();
		refreshDevices();
	}

	public void refreshDevices() throws IOException, JadbException {
		androidDevices = adb.getDevices().stream().map(a -> {
			try {
				return AndroidDevice.createFrom(a);
			} catch (Throwable e) {
				e.printStackTrace();
				return null;
			}
		})
			.filter(a -> a != null)
			.toList();
	}

	@Override
	protected void initImGui(Configuration config) {
		super.initImGui(config);
		ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);

		// Use a nice font on Windows
		if (System.getProperty("os.name").contains("Windows")) {
			font = ImGui.getIO().getFonts().addFontFromFileTTF("C:\\Windows\\Fonts\\consola.ttf", 14);
		}
	}

	@Override
	protected void run() {
		super.run();
		if (selectedInput != null) selectedInput.stopPolling();
	}

	@Override
	public void process() {
		if (font != null) ImGui.pushFont(font);
		ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), ImGuiDockNodeFlags.PassthruCentralNode);

		ImGui.beginMainMenuBar();
		if (ImGui.beginMenu("Window")) {
			// @formatter:off
			if (ImGui.menuItem("Devices", selectDevicesWindow.get())) selectDevicesWindow.set(!selectDevicesWindow.get());
			if (ImGui.menuItem("Inspector", inspectorWindow.get())) inspectorWindow.set(!inspectorWindow.get());
			if (ImGui.menuItem("Visual Inspector", visualInspectorWindow.get())) visualInspectorWindow.set(!visualInspectorWindow.get());
			// @formatter:on
			ImGui.endMenu();
		}
		ImGui.endMainMenuBar();

		ImGui.setNextWindowSize(200f, 500f);
		selectWindow();

		ImGui.setNextWindowSize(400f, 500f);
		inspectorWindow();

		ImGui.setNextWindowSize(600f, 500f);
		visualInspectorWindow();
		if (font != null) ImGui.popFont();
	}

	private void selectWindow() {
		float lineHeight = ImGui.getTextLineHeightWithSpacing();

		if (!selectDevicesWindow.get()) return;
		if (ImGui.begin("Devices", selectDevicesWindow)) {
			if (ImGui.button("Refresh")) {
				if (selectedDevice != null) selectAndroidDevice(null);
				try {
					refreshDevices();
				} catch (IOException | JadbException e) {
					e.printStackTrace();
				}
			}

			ImGui.text("Instructions:\n"
				+ "1. Connect your phone with USB debugging\n"
				+ "2. Accept debugging prompts (if any)\n"
				+ "3. Click 'Refresh' button");
			ImGui.spacing();
			ImGui.text("Select Android device below:");
			if (ImGui.beginListBox("##AndroidDevices", ImGui.getWindowWidth() - 15, lineHeight * 5)) {
				for (AndroidDevice android : androidDevices) {
					if (ImGui.selectable(android.getAdbDevice().getSerial(), selectedDevice == android))
						selectAndroidDevice(android);
				}
				ImGui.endListBox();
			}

			if (selectedDevice != null) {
				ImGui.spacing();
				ImGui.text("Select Input device below:");
				float height = ImGui.getWindowSizeY() - ImGui.getCursorPosY() - 8;
				if (ImGui.beginListBox("##InputDevices", ImGui.getWindowWidth() - 15, height)) {
					for (InputDevice input : selectedDevice.getInputDevices()) {
						if (ImGui.selectable(input.getName(), selectedInput == input)) selectInputDevice(input);
					}
					ImGui.endListBox();
				}
			}
		}
		ImGui.end();
	}

	private void inspectorWindow() {
		if (!inspectorWindow.get()) return;
		if (ImGui.begin("Inspector", inspectorWindow)) {
			ImGui.text("Android: " + (selectedDevice != null
				? selectedDevice.getAdbDevice().getSerial()
				: "<not selected>"));
			ImGui.text("Input: " + (selectedInput != null
				? selectedInput.getName()
				: "<not selected>"));

			if (lastEvent == null) {
				ImGui.text("Waiting for event...");
			} else {
				for (EventProperty prop : lastEvent.getProperties()) {
					ImGui.labelText(prop.getName(), Integer.toString(lastEvent.get(prop)));
				}

				ImGui.text("Extra:");
				ImGui.labelText("Polling Rate (Now)", "%.2f Hz".formatted(calculatedRate));
				ImGui.labelText("Polling Rate (Avg)", "%.2f Hz".formatted(averageRate));
			}
		}
		ImGui.end();
	}

	private void visualInspectorWindow() {
		float lineHeight = ImGui.getTextLineHeight();

		if (!visualInspectorWindow.get()) return;
		if (ImGui.begin("Visual Inspector", visualInspectorWindow)) {
			if (ImGui.button("Clear")) trail.clear();
			ImGui.sameLine();
			if (ImGui.button("Export CSV")) ImGui.openPopup("exportCSV");
			exportCsvFromVisualInspectorPopup();
			ImGui.sameLine();
			if (ImGui.checkbox("Z Axis/Pressure", enableZ)) enableZ = !enableZ;
			ImGui.sameLine();
			if (ImGui.checkbox("Tilt", enableTilt)) enableTilt = !enableTilt;
			ImGui.sameLine();
			if (ImGui.checkbox("Fading Trail", enableFadingTrail)) enableFadingTrail = !enableFadingTrail;

			ImGui.sliderInt("Trail Length", trailLength, 10, 1000);
			ImGui.sliderInt("Max Z Radius", visualizeRadius, 1, 500);
			ImGui.sliderFloat("Z Radius Exponent", visualizeExponent, 0.1f, 10f, ImGuiSliderFlags.Logarithmic);

			ImDrawList drawList = ImGui.getWindowDrawList();
			ImVec2 cursor = ImGui.getCursorScreenPos();
			ImVec2 size = ImGui.getWindowSize().minus(ImGui.getCursorPosX() + 8, ImGui.getCursorPosY() + 8);
			drawList.addRectFilled(cursor.x, cursor.y, cursor.x + size.x, cursor.y + size.y, ImColor.rgb(31, 31, 31));
			drawList.addRect(cursor.x, cursor.y, cursor.x + size.x, cursor.y + size.y, ImColor.rgb(127, 127, 127));

			if (selectedInput != null) {
				InputDeviceMetrics metrics = findMetricsFrom(selectedInput);
				InputDeviceDataPoint dataPoint = findDataPointFrom(lastEvent);

				if (metrics.sizeX != 0 && metrics.sizeY != 0) {
					// Letterboxing
					ImVec2 letterboxedSize;
					ImVec2 letterboxedPos;

					if (size.x / size.y > metrics.sizeX / (float) metrics.sizeY) {
						// Parent is wider than child
						letterboxedSize = new ImVec2(size.y * metrics.sizeX / metrics.sizeY, size.y);
						letterboxedPos = new ImVec2((size.x - letterboxedSize.x) / 2, 0);
					} else {
						// Child is wider than parent
						letterboxedSize = new ImVec2(size.x, size.x * metrics.sizeY / metrics.sizeX);
						letterboxedPos = new ImVec2(0, (size.y - letterboxedSize.y) / 2);
					}

					letterboxedPos.plus(cursor);
					drawList.addRectFilled(
						letterboxedPos,
						letterboxedPos.clone().plus(letterboxedSize),
						ImColor.rgb(63, 63, 63));
					drawList.addRect(
						letterboxedPos,
						letterboxedPos.clone().plus(letterboxedSize),
						ImColor.rgb(255, 255, 255));
					drawList.addLine(
						letterboxedPos.x + dataPoint.x * letterboxedSize.x / metrics.sizeX,
						letterboxedPos.y,
						letterboxedPos.x + dataPoint.x * letterboxedSize.x / metrics.sizeX,
						letterboxedPos.y + letterboxedSize.y,
						ImColor.rgb(127, 127, 127));
					drawList.addLine(
						letterboxedPos.x,
						letterboxedPos.y + dataPoint.y * letterboxedSize.y / metrics.sizeY,
						letterboxedPos.x + letterboxedSize.x,
						letterboxedPos.y + dataPoint.y * letterboxedSize.y / metrics.sizeY,
						ImColor.rgb(127, 127, 127));

					// Trails
					for (int i = 0; i < trail.size(); i++) {
						InputDeviceDataPoint point = trail.get(i);
						int alpha = enableFadingTrail ? i * 255 / trail.size() : 255;
						float dispX = letterboxedPos.x + point.x * letterboxedSize.x / metrics.sizeX;
						float dispY = letterboxedPos.y + point.y * letterboxedSize.y / metrics.sizeY;

						drawList.addCircleFilled(dispX, dispY,
							metrics.sizeZ != 0 && enableZ
								? (int) (Math.pow(point.z / (double) metrics.sizeZ, visualizeExponent[0])
									* visualizeRadius[0])
								: 1,
							ImColor.rgba(255, 255, 255, alpha));

						if (i > 0) {
							InputDeviceDataPoint prev = trail.get(i - 1);
							float prevX = letterboxedPos.x + prev.x * letterboxedSize.x / metrics.sizeX;
							float prevY = letterboxedPos.y + prev.y * letterboxedSize.y / metrics.sizeY;

							drawList.addLine(
								prevX, prevY,
								dispX, dispY,
								ImColor.rgba(255, 64, 64, alpha));
						}

						if (enableTilt && (point.tiltX != 0 || point.tiltY != 0)) drawList.addLine(
							dispX, dispY,
							dispX + point.tiltX, dispY + point.tiltY,
							ImColor.rgba(64, 64, 255, alpha));
					}

					// @formatter:off
					drawList.addText(cursor.x + 4, cursor.y + 4, ImColor.rgb(255, 127, 127), "X: %d/%d".formatted(dataPoint.x, metrics.sizeX));
					drawList.addText(cursor.x + 4, cursor.y + 4 + lineHeight, ImColor.rgb(127, 255, 127), "Y: %d/%d".formatted(dataPoint.y, metrics.sizeY));
					drawList.addText(cursor.x + 4, cursor.y + 4 + lineHeight * 2, ImColor.rgb(127, 127, 255), "Z: %d/%d".formatted(dataPoint.z, metrics.sizeZ));
					// @formatter:on
				}
			}
		}
		ImGui.end();
	}

	private void exportCsvFromVisualInspectorPopup() {
		if (ImGui.beginPopup("exportCSV")) {
			exportCsvPopup.imgui(420f, 200f, target -> {
				try (FileOutputStream stream = new FileOutputStream(target)) {
					stream.write(InputDeviceDataPoint.CSV_HEADER.getBytes(StandardCharsets.UTF_8));
					for (InputDeviceDataPoint e : trail) {
						stream.write(("\n" + e.asCSVLine()).getBytes(StandardCharsets.UTF_8));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				ImGui.closeCurrentPopup();
			});
			ImGui.endPopup();
		}
	}

	public void selectAndroidDevice(AndroidDevice android) {
		if (selectedDevice == android) return;
		selectedDevice = android;
		selectInputDevice(null);
	}

	public void selectInputDevice(InputDevice input) {
		if (selectedInput == input) return;

		if (selectedInput != null) {
			selectedInput.removeListener(this::handleInputEvent);
			selectedInput.stopPolling();
		}

		selectedInput = input;
		lastEvent = null;
		calculatedRate = null;
		averageRate = null;
		rateHistory.clear();
		trail.clear();

		if (input != null) {
			input.addListener(this::handleInputEvent);
			input.startPolling();
		}
	}

	private void handleInputEvent(InputEvent event) {
		if (lastEvent != null) {
			BigDecimal tsLast = lastEvent.timestamp();
			BigDecimal tsNow = event.timestamp();

			if (!tsNow.equals(tsLast)) {
				calculatedRate = BigDecimal.ONE.divide(tsNow.subtract(tsLast), 6, RoundingMode.DOWN);
				rateHistory.add(calculatedRate);
				while (rateHistory.size() > 100) rateHistory.remove(0);

				averageRate = rateHistory.get(0);
				for (int i = 1; i < rateHistory.size(); i++) averageRate = averageRate.add(rateHistory.get(i));
				averageRate = averageRate.divide(new BigDecimal(rateHistory.size()), 6, RoundingMode.DOWN);
			}
		}

		lastEvent = event;
		InputDeviceDataPoint dataPoint = findDataPointFrom(event);
		trail.add(dataPoint);
		while (trail.size() > trailLength[0]) trail.remove(0);
	}

	private InputDeviceMetrics findMetricsFrom(InputDevice input) {
		PropertyInfo propAbsX = input.getProperties().get(EventProperty.ABS_X);
		PropertyInfo propAbsY = input.getProperties().get(EventProperty.ABS_Y);
		PropertyInfo propAbsZ = input.getProperties().get(EventProperty.ABS_Z);
		PropertyInfo propAbsP = input.getProperties().get(EventProperty.ABS_PRESSURE);
		PropertyInfo propAbsMtX = input.getProperties().get(EventProperty.ABS_MT_POSITION_X);
		PropertyInfo propAbsMtY = input.getProperties().get(EventProperty.ABS_MT_POSITION_Y);
		PropertyInfo propAbsMtMJ = input.getProperties().get(EventProperty.ABS_MT_TOUCH_MAJOR);

		int x = propAbsMtX instanceof RangePropertyInfo range
			? range.max()
			: propAbsX instanceof RangePropertyInfo range ? range.max()
			: 0;
		int y = propAbsMtY instanceof RangePropertyInfo range
			? range.max() : propAbsY instanceof RangePropertyInfo range ? range.max()
			: 0;
		int z = propAbsZ instanceof RangePropertyInfo range
			? range.max()
			: propAbsP instanceof RangePropertyInfo range ? range.max()
			: propAbsMtMJ instanceof RangePropertyInfo range ? range.max()
			: 0;

		return new InputDeviceMetrics(x, y, z);
	}

	private InputDeviceDataPoint findDataPointFrom(InputEvent event) {
		if (event == null) return new InputDeviceDataPoint(0, 0, 0, 0, 0);

		int x = 0, y = 0, z = 0, tiltX = 0, tiltY = 0;
		// @formatter:off
		if (event.getProperties().contains(EventProperty.ABS_X)) x = event.get(EventProperty.ABS_X);
		if (event.getProperties().contains(EventProperty.ABS_Y)) y = event.get(EventProperty.ABS_Y);
		if (event.getProperties().contains(EventProperty.ABS_Z)) z = event.get(EventProperty.ABS_Z);
		if (event.getProperties().contains(EventProperty.ABS_TILT_X)) tiltX = event.get(EventProperty.ABS_TILT_X);
		if (event.getProperties().contains(EventProperty.ABS_TILT_Y)) tiltY = event.get(EventProperty.ABS_TILT_Y);
		if (event.getProperties().contains(EventProperty.ABS_PRESSURE)) z = event.get(EventProperty.ABS_PRESSURE);
		if (event.getProperties().contains(EventProperty.ABS_MT_POSITION_X)) x = event.get(EventProperty.ABS_MT_POSITION_X);
		if (event.getProperties().contains(EventProperty.ABS_MT_POSITION_Y)) y = event.get(EventProperty.ABS_MT_POSITION_Y);
		if (event.getProperties().contains(EventProperty.ABS_MT_TOUCH_MAJOR)) z = event.get(EventProperty.ABS_MT_TOUCH_MAJOR);
		// @formatter:on

		return new InputDeviceDataPoint(x, y, z, tiltX, tiltY);
	}

	private static record InputDeviceMetrics(int sizeX, int sizeY, int sizeZ) {
	}

	private static record InputDeviceDataPoint(int x, int y, int z, int tiltX, int tiltY) {
		public String asCSVLine() {
			return "%d,%d,%d,%d,%d".formatted(x, y, z, tiltX, tiltY);
		}

		public static final String CSV_HEADER = "X,Y,Z,TiltX,TiltY";
	}
}
