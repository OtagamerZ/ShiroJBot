package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;

public class MultMod extends ValueMod {
	public MultMod(Number value) {
		super(value);
	}

	public MultMod(Drawable<?> source, Number value) {
		super(source, value);
	}

	public MultMod(Number value, int expiration) {
		super(value, expiration);
	}

	public MultMod(Drawable<?> source, Number value, int expiration) {
		super(source, value, expiration);
	}
}
