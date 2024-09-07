# Android Digitizer
Read raw digitizer data from your Android device with USB debugging (or Wifi debugging, if you have ADB installed).

This project utilizes ADB shell and `cat` utility to obtain the raw data of your Android device (by `cat`ing `/dev/input/event#` and stream it to computer). This works without having to install a special application on your Android device (and it works system-wide too!).

An alternative way to capture raw digitizer data in your Android app would be launching binary with Shizuku, which requires one-time USB debugging setup, but it works until you restart your device.

## Usage
```java
JadbConnection adb;
JadbDevice device;

AndroidDevice device2 = AndroidDevice.createFrom(device);
InputDevice penDevice = device2.getInputDevices().stream().filter(i -> i.getName().contains("pen")).getFirst().get();

int maxX = penDevice.getProperties().get(EventProperty.ABS_X) instanceof RangePropertyInfo r ? r.max() : 1024;
int maxY = penDevice.getProperties().get(EventProperty.ABS_Y) instanceof RangePropertyInfo r ? r.max() : 1024;
int maxP = penDevice.getProperties().get(EventProperty.ABS_PRESSURE) instanceof RangePropertyInfo r ? r.max() : 1024;

penDevice.startPolling();
penDevice.addListener(event -> {
	int x = event.get(EventProperty.ABS_X);
	int y = event.get(EventProperty.ABS_Y);
	int pressure = event.get(EventProperty.ABS_PRESSURE);
	myCanvas.draw(x * myCanvas.width / maxX, y * myCanvas.height / maxY, pressure * 100 / maxP);
});

// When exiting
penDevice.stopPolling();
```

See [AndroidDigitizerMain](android-digitizer-demo/src/main/java/me/nahkd/adigitizer/demo/AndroidDigitizerMain.java) for digitizer testing app example.

## But I want to get the testing app
See [Releases](https://github.com/nahkd123/android-digitizer/releases) and find the `android-digitizer-demo-0.0.1-SNAPSHOT-jar-with-dependencies.jar` file.

Additionally, you can import the Maven project to your IDE, then launch the `me.nahkd.adigitizer.demo.AndroidDigitizerMain` main class.

## License
- Android Digitizer (this repo) is licensed under MIT
- [jadb](https://github.com/vidstige/jadb) is licensed under Apache 2.0
- [imgui-java](https://github.com/SpaiR/imgui-java) is licensed under MIT