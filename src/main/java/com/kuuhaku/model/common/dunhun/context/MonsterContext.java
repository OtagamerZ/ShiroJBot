package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.records.dunhun.Loot;

public class MonsterContext extends ActorContext {
	private final Loot loot;
	private final double mult;

	public MonsterContext(Actor<?> actor, Loot loot, double mult) {
		super(actor);
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
