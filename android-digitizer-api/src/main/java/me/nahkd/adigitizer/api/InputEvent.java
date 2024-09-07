package me.nahkd.adigitizer.api;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import me.nahkd.adigitizer.api.prop.EventProperty;

/**
 * <p>
 * Represent an input event.
 * </p>
 */
public interface InputEvent {
	BigInteger WHOLE_SECOND_IN_MICRO = new BigInteger("1000000");

	long timestampSecond();

	long timestampMicrosecond();

	default BigDecimal timestamp() {
		BigInteger bi = BigInteger.valueOf(timestampSecond())
			.multiply(WHOLE_SECOND_IN_MICRO)
			.add(BigInteger.valueOf(timestampMicrosecond()));
		return new BigDecimal(bi, 6);
	}

	Collection<EventProperty> getProperties();

	int get(EventProperty property);
}
