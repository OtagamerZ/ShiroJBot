package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.records.dunhun.CombatContext;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;

public abstract class EffectBase {
	private final EffectContext<?> source;
	private final Actor<?> owner;
	private final BiConsumer<EffectBase, CombatContext> effect;
	private boolean closed = false;
	private boolean lock = false;

	public EffectBase(EffectContext<?> source, Actor<?> owner, ThrowingBiConsumer<EffectBase, CombatContext> effect) {
		this.source = source;
		this.owner = owner;
		this.effect = effect;
	}

	public EffectContext<?> getSource() {
		return source;
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
		if (o == null || getClass() != o.getClass()) return false;
		EffectBase that = (EffectBase) o;
		return Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(source);
	}
}
