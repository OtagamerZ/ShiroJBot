package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

public class PersistentEffect extends TriggeredEffect {
	public PersistentEffect(EffectContext<?> source, Actor<?> owner, ThrowingBiConsumer<EffectBase, CombatContext> effect) {
		super(source, owner, -1, effect, Trigger.ON_TURN_BEGIN);
	}
}
