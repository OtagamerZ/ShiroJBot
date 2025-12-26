package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;

import java.util.Objects;

public class EffectContext<T> {
	private final Dunhun game;
	private final T source;

	public EffectContext(Dunhun game, T source) {
		this.game = game;
		this.source = source;
	}

	public Dunhun getGame() {
		return game;
	}

	public T getSource() {
		return source;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		EffectContext<?> that = (EffectContext<?>) o;
		return Objects.equals(game, that.game) && Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(game, source);
	}
}
