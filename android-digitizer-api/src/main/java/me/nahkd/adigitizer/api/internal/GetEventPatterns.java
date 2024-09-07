package me.nahkd.adigitizer.api.internal;

import java.util.regex.Pattern;

public class GetEventPatterns {
	// @formatter:off
	public static final Pattern L0_ADD_DEVICE = Pattern.compile("^add device \\d+: (?<device>\\/dev\\/input\\/event\\d+)$");
	public static final Pattern L1_PROPERTY = Pattern.compile("^  (?<name>.+?):? +(?:(?<integer>[A-Fa-f0-9]{4})|(?:\"(?<string>.*?)\")|(?<version>\\d+\\.\\d+\\.\\d+))$");
	public static final Pattern L1_SECTION = Pattern.compile("^  (?<name>.+):$");
	public static final Pattern L2_EVENTS = Pattern.compile("^ {4}(?:[A-Z]{2,3} +\\((?<type>[A-Fa-f0-9]{4})\\): | {12})(?:(?<enum>(?:[A-Fa-f0-9]{4} +)+)|(?<code>[A-Fa-f0-9]{4})  : value (?<value>-?\\d+), min (?<min>-?\\d+), max (?<max>-?\\d+), fuzz (?<fuzz>-?\\d+), flat (?<flat>-?\\d+), resolution (?<resolution>-?\\d+)|)$");
	public static final Pattern L2_INPUT_PROPS = Pattern.compile("^ {4}(?:<none>|(?<value>\\w+))$");
	// @formatter:on
}
