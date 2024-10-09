package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;

import java.util.function.BiConsumer;

public final class TriggeredEffect extends EffectBase {
	private final Trigger trigger;
	private int limit;

	public TriggeredEffect(Actor target, Trigger trigger, int duration, int limit, BiConsumer<EffectBase, Actor> effect) {
		super(target, duration, effect);
		this.trigger = trigger;
		this.limit = limit;
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public int getLimit() {
		return limit;
	}

	public boolean decLimit() {
		if (limit > 0) limit--;
		return limit == 0;
	}
}
