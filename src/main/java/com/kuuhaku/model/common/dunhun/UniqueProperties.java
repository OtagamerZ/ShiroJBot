package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.dunhun.context.EffectContext;

import java.util.Objects;

public class UniqueProperties<T> extends EffectProperties<T> {
	public UniqueProperties(EffectContext<T> owner) {
		super(owner);
	}

	public UniqueProperties(EffectContext<T> owner, int duration) {
		super(owner, duration);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		UniqueProperties<?> that = (UniqueProperties<?>) o;
		return Objects.equals(getOwner(), that.getOwner());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getOwner());
	}
}
