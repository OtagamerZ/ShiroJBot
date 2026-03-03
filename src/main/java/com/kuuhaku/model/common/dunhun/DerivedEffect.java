package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

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
}
