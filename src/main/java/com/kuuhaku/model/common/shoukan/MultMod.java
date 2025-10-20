package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;

public class MultMod extends ValueMod {
	public MultMod(double value) {
		super(value);
	}

	public MultMod(Drawable<?> source, double value) {
		super(source, value);
	}

	public MultMod(double value, int expiration) {
		super(value, expiration);
	}

	public MultMod(Drawable<?> source, double value, int expiration) {
		super(source, value, expiration);
	}
}
