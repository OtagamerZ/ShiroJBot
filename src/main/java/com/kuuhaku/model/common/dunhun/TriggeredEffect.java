package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

public class TriggeredEffect extends EffectBase {
	private final Trigger[] triggers;
	private int limit;

	public TriggeredEffect(Actor<?> owner, int limit, ThrowingBiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		super(owner, effect);
		this.triggers = triggers;
		this.limit = limit;
	}

	public Trigger[] getTriggers() {
		return triggers;
	}

	public int getLimit() {
		return limit;
	}

	public boolean decLimit() {
		if (limit > 0) limit--;
		return limit == 0;
	}

	@Override
	public boolean isClosed() {
		return super.isClosed() || limit == 0;
	}
}
