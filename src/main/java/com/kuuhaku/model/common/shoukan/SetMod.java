package com.kuuhaku.model.common.shoukan;

import java.util.function.Supplier;

public class SetMod extends ValueMod {
	public SetMod(Number value) {
		super(value);
	}

	public SetMod(Supplier<Number> value) {
		super(value);
	}

	public SetMod(Object source, Number value) {
		super(source, value);
	}

	public SetMod(Object source, Supplier<Number> value) {
		super(source, value);
	}

	public SetMod(Number value, int expiration) {
		super(value, expiration);
	}

	public SetMod(Supplier<Number> value, int expiration) {
		super(value, expiration);
	}

	public SetMod(Object source, Number value, int expiration) {
		super(source, value, expiration);
	}

	public SetMod(Object source, Supplier<Number> value, int expiration) {
		super(source, value, expiration);
	}
}
