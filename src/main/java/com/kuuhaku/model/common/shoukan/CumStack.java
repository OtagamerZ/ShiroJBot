package com.kuuhaku.model.common.shoukan;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class CumStack implements Iterable<ValueMod> {
	private final CumValue flat = CumValue.flat();
	private final CumValue increased = CumValue.flat();
	private final CumValue more = CumValue.mult();

	public double get() {
		return get(0);
	}

	public double get(double base) {
		return (base + flat.get()) * (1 + increased.get()) * more.get();
	}

	public CumValue getFlat() {
		return flat;
	}

	public CumValue getIncreased() {
		return increased;
	}

	public CumValue getMore() {
		return more;
	}

	public List<ValueMod> values() {
		return Stream.of(flat, increased, more)
				.map(CumValue::values)
				.flatMap(Set::stream)
				.toList();
	}

	@Override
	public @NotNull Iterator<ValueMod> iterator() {
		return values().iterator();
	}
}
