package me.nahkd.adigitizer.api.prop;

public record RangePropertyInfo(EventProperty property, int min, int max, int fuzz) implements PropertyInfo {
	@Override
	public final String toString() {
		return "%s: Range [%d; %d], Fuzz %d".formatted(property, min, max, fuzz);
	}

	public double mapToDouble(int input) {
		return (input - min) / (double) (max - min);
	}
}
