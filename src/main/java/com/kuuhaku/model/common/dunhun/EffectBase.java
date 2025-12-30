package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.records.dunhun.CombatContext;

import javax.annotation.Nullable;

public abstract class EffectBase {
	private static final ThrowingBiConsumer<EffectBase, CombatContext> NOTHING = (_, _) -> {
	};

	private final EffectContext<?> source;
	private final Actor<?> owner;
	private final ThrowingBiConsumer<EffectBase, CombatContext> effect;
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

	public ThrowingBiConsumer<EffectBase, CombatContext> getEffect() {
		if (isClosed()) return NOTHING;

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
}
