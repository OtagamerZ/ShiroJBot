package com.kuuhaku.model.common.shoukan;

public class FlatMod extends ValueMod {
	public FlatMod(Number value) {
		super(value);
	}

	public FlatMod(Object source, Number value) {
		super(source, value);
	}

	public FlatMod(Number value, int expiration) {
		super(value, expiration);
	}

	public FlatMod(Object source, Number value, int expiration) {
		super(source, value, expiration);
	}
}
