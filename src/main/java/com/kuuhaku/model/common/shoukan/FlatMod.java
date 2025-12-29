package com.kuuhaku.model.common.shoukan;

import java.util.function.Supplier;

public class FlatMod extends ValueMod {
	public FlatMod(Number value) {
		super(value);
	}

	public FlatMod(Supplier<Number> value) {
		super(value);
	}

	public FlatMod(Object source, Number value) {
		super(source, value);
	}

	public FlatMod(Object source, Supplier<Number> value) {
		super(source, value);
	}

	public FlatMod(Number value, int expiration) {
		super(value, expiration);
	}

	public FlatMod(Supplier<Number> value, int expiration) {
		super(value, expiration);
	}

	public FlatMod(Object source, Number value, int expiration) {
		super(source, value, expiration);
	}

	public FlatMod(Object source, Supplier<Number> value, int expiration) {
		super(source, value, expiration);
	}
}
