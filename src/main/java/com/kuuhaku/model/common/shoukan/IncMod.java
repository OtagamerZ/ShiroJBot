package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;

public class IncMod extends ValueMod {
	public IncMod(Number value) {
		super(value);
	}

	public IncMod(Drawable<?> source, Number value) {
		super(source, value);
	}

	public IncMod(Number value, int expiration) {
		super(value, expiration);
	}

	public IncMod(Drawable<?> source, Number value, int expiration) {
		super(source, value, expiration);
	}
}
