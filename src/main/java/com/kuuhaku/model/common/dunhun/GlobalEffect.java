package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

public class GlobalEffect extends TriggeredEffect {
	private int turns;

	public GlobalEffect(EffectContext<?> source, Actor<?> owner, int limit, int turns, ThrowingBiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		super(source, owner, limit, effect, triggers);
		this.turns = turns;
	}

	public int getTurns() {
		return turns;
	}

	public void setTurns(int turns) {
		this.turns = Math.max(0, turns);
	}

	public void decTurn() {
		if (turns > 0) turns--;
	}

	@Override
	public boolean isClosed() {
		return turns == 0 || super.isClosed();
	}
}
