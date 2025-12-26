package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.persistent.dunhun.Affix;

import java.util.Objects;

public class AffixProperties<T> extends EffectProperties<T> {
	private final Affix affix;

	public AffixProperties(Affix affix, EffectContext<T> owner) {
		super(owner);
		this.affix = affix;
	}

	public AffixProperties(Affix affix, EffectContext<T> owner, int duration) {
		super(owner, duration);
		this.affix = affix;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		AffixProperties<?> that = (AffixProperties<?>) o;
		return Objects.equals(affix, that.affix);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(affix);
	}
}
