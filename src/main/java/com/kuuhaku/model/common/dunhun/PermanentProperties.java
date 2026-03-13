package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.dunhun.context.EffectContext;

public class PermanentProperties<T> extends EffectProperties<T> {
	private final Actor<?> owner;

	public PermanentProperties(EffectContext<T> context) {
		this(context, null);
	}

	public PermanentProperties(EffectContext<T> context, Actor<?> owner) {
		super(context);
		this.owner = owner;
	}

	@Override
	public boolean isSafeToRemove() {
		if (owner == null) return false;

		return owner.isDisposed();
	}

	public boolean isActive() {
		return !isSafeToRemove() && !(owner != null && owner.isOutOfCombat());
	}
}
