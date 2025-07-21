package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

public class PersistentEffect extends TriggeredEffect {
	public PersistentEffect(Actor<?> owner, ThrowingBiConsumer<EffectBase, CombatContext> effect) {
		super(owner, -1, effect, Trigger.ON_TURN_BEGIN);
	}
}
