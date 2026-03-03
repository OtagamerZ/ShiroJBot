package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

import java.util.Objects;

public class DerivedEffect extends TriggeredEffect {
	private final EffectBase parent;

	public DerivedEffect(EffectBase parent, ThrowingBiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		super(parent.getSource(), parent.getOwner(), -1, effect, triggers);
		this.parent = parent;
	}

	public EffectBase getParent() {
		return parent;
	}

	@Override
	public boolean isClosed() {
		return super.isClosed() || parent.isClosed();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		DerivedEffect that = (DerivedEffect) o;
		return Objects.equals(parent, that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), parent);
	}
}
