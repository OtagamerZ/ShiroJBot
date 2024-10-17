package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.records.dunhun.CombatContext;

import java.util.function.BiConsumer;

public final class TriggeredEffect extends EffectBase {
	private final Trigger[] triggers;
	private int limit;
	private boolean lock;

	public TriggeredEffect(Actor owner, int duration, int limit, BiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		super(owner, duration, effect);
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
