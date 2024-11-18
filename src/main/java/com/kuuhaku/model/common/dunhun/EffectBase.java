package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.records.dunhun.CombatContext;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public abstract class EffectBase {
	public final long SERIAL = ThreadLocalRandom.current().nextLong();
	private final Actor owner;
	private final BiConsumer<EffectBase, CombatContext> effect;
	private int duration;

	public EffectBase(Actor owner, int duration, ThrowingBiConsumer<EffectBase, CombatContext> effect) {
		this.owner = owner;
		this.duration = duration;
		this.effect = effect;
	}

	@Nullable
	public Actor getOwner() {
		return owner;
	}

	public int getDuration() {
		return duration;
	}

	public boolean decDuration() {
		if (duration > 0) duration--;
		return duration == 0;
	}

	public BiConsumer<EffectBase, CombatContext> getEffect() {
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
