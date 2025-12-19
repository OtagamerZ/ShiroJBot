package com.kuuhaku.model.common.shoukan;

public class MultMod extends ValueMod {
	public MultMod(Number value) {
		super(value);
	}

	public MultMod(Object source, Number value) {
		super(source, value);
	}

	public MultMod(Number value, int expiration) {
		super(value, expiration);
	}

	public MultMod(Object source, Number value, int expiration) {
		super(source, value, expiration);
	}
}
