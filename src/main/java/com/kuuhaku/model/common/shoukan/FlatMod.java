package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;

public class FlatMod extends ValueMod {
	public FlatMod(Number value) {
		super(value);
	}

	public FlatMod(Drawable<?> source, Number value) {
		super(source, value);
	}

	public FlatMod(Number value, int expiration) {
		super(value, expiration);
	}

	public FlatMod(Drawable<?> source, Number value, int expiration) {
		super(source, value, expiration);
	}
}
