package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.game.Dunhun;

public class EffectContext {
	private final Dunhun game;

	public EffectContext(Dunhun game) {
		this.game = game;
	}

	public Dunhun getGame() {
		return game;
	}
}
