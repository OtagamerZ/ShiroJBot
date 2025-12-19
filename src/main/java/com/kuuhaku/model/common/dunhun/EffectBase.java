package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.records.dunhun.CombatContext;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public abstract class EffectBase {
	public final long SERIAL = ThreadLocalRandom.current().nextLong();
	private final Actor<?> owner;
	private final BiConsumer<EffectBase, CombatContext> effect;
	private boolean closed = false;
	private boolean lock = false;

	public EffectBase(Actor<?> owner, ThrowingBiConsumer<EffectBase, CombatContext> effect) {
		this.owner = owner;
		this.effect = effect;
	}

	@Nullable
	public Actor<?> getOwner() {
		return owner;
	}

	public BiConsumer<EffectBase, CombatContext> getEffect() {
		return effect;
	}

	public boolean isClosed() {
		return closed || (owner != null && !owner.getBinding().isBound());
	}

	public void close() {
		closed = true;
	}

	public boolean isLocked() {
		return lock || isClosed();
	}

	public void lock() {
		lock = true;
	}

	public void unlock() {
		lock = false;
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
