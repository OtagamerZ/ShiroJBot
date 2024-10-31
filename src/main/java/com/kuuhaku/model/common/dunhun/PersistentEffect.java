package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.records.dunhun.CombatContext;

public class PersistentEffect extends EffectBase {
	public PersistentEffect(Actor owner, int duration, ThrowingBiConsumer<EffectBase, CombatContext> effect) {
		super(owner, duration, effect);
	}
}
