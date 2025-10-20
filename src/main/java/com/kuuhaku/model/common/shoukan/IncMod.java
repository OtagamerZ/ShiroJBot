package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;

public class IncMod extends ValueMod {
	public IncMod(double value) {
		super(value);
	}

	public IncMod(Drawable<?> source, double value) {
		super(source, value);
	}

	public IncMod(double value, int expiration) {
		super(value, expiration);
	}

	public IncMod(Drawable<?> source, double value, int expiration) {
		super(source, value, expiration);
	}
}
