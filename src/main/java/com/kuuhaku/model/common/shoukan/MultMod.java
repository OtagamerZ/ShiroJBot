package com.kuuhaku.model.common.shoukan;

import java.util.function.Supplier;

public class MultMod extends ValueMod {
	public MultMod(Number value) {
		super(value);
	}

	public MultMod(Supplier<Number> value) {
		super(value);
	}

	public MultMod(Object source, Number value) {
		super(source, value);
	}

	public MultMod(Object source, Supplier<Number> value) {
		super(source, value);
	}

	public MultMod(Number value, int expiration) {
		super(value, expiration);
	}

	public MultMod(Supplier<Number> value, int expiration) {
		super(value, expiration);
	}

	public MultMod(Object source, Number value, int expiration) {
		super(source, value, expiration);
	}

	public MultMod(Object source, Supplier<Number> value, int expiration) {
		super(source, value, expiration);
	}
}
