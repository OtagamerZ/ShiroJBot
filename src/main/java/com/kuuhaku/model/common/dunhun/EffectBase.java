package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public abstract class EffectBase {
	public final long SERIAL = ThreadLocalRandom.current().nextLong();
	private final Actor target;
	private final BiConsumer<EffectBase, Actor> effect;
	private int duration;

	public EffectBase(Actor target, int duration, BiConsumer<EffectBase, Actor> effect) {
		this.target = target;
		this.duration = duration;
		this.effect = effect;
	}

	public Actor getTarget() {
		return target;
	}

	public int getDuration() {
		return duration;
	}

	public boolean decDuration() {
		if (duration > 0) duration--;
		return duration == 0;
	}

	public BiConsumer<EffectBase, Actor> getEffect() {
		return effect;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EffectBase that = (EffectBase) o;
		return SERIAL == that.SERIAL;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(SERIAL);
	}
}
