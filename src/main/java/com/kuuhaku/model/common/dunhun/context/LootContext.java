package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.MonsterBase;
import com.kuuhaku.model.records.dunhun.Loot;

public class LootContext extends ActorContext {
	private final Loot loot;
	private final double mult;

	public LootContext(MonsterBase<?> actor, Loot loot, double mult) {
		super(actor, null);
		this.loot = loot;
		this.mult = mult;
	}

	public Loot getLoot() {
		return loot;
	}

	public double getMult() {
		return mult;
	}
}
