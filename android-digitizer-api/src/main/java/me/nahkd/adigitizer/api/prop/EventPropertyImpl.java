package me.nahkd.adigitizer.api.prop;

record EventPropertyImpl(short type, short code, String name) implements EventProperty {
	public EventPropertyImpl(int type, int code, String name) {
		this((short) type, (short) code, name);
	}

	@Override
	public String getName() { return name; }

	@Override
	public final String toString() {
		return name;
	}
}
