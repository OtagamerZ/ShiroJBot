package com.kuuhaku.model.common.shoukan;

import java.util.function.Supplier;

public class IncMod extends ValueMod {
	public IncMod(Number value) {
		super(value);
	}

	public IncMod(Supplier<Number> value) {
		super(value);
	}

	public IncMod(Object source, Number value) {
		super(source, value);
	}

	public IncMod(Object source, Supplier<Number> value) {
		super(source, value);
	}

	public IncMod(Number value, int expiration) {
		super(value, expiration);
	}

	public IncMod(Supplier<Number> value, int expiration) {
		super(value, expiration);
	}

	public IncMod(Object source, Number value, int expiration) {
		super(source, value, expiration);
	}

	public IncMod(Object source, Supplier<Number> value, int expiration) {
		super(source, value, expiration);
	}
}
