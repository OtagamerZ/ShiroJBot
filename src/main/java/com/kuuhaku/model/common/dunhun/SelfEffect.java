package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

public class SelfEffect extends EffectBase{
	private final Trigger[] triggers;
	private boolean lock;

	public SelfEffect(Actor owner, ThrowingBiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		super(owner, -1, effect);
		this.triggers = triggers;
	}

	public Trigger[] getTriggers() {
		return triggers;
	}

	public boolean isLocked() {
		return lock;
	}

	public void lock() {
		lock = true;
	}

	public void unlock() {
		lock = false;
	}
}
