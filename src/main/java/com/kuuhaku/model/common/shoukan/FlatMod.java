package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;

public class FlatMod extends ValueMod {
	public FlatMod(double value) {
		super(value);
	}

	public FlatMod(Drawable<?> source, double value) {
		super(source, value);
	}

	public FlatMod(double value, int expiration) {
		super(value, expiration);
	}

	public FlatMod(Drawable<?> source, double value, int expiration) {
		super(source, value, expiration);
	}
}
