package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Gear;

import java.util.List;

public class GearContext extends EffectContext {
	private final Gear gear;
	private final Actor<?> actor;
	private final List<Integer> values;

	public GearContext(Gear gear) {
		super(null);
		this.gear = gear;
		this.actor = null;
		this.values = List.of();
	}

	public GearContext(Actor<?> actor, Gear gear, List<Integer> values) {
		super(actor.getGame());
		this.gear = gear;
		this.actor = actor;
		this.values = values;
	}

	public Gear getGear() {
		return gear;
	}

	public Actor<?> getActor() {
		return actor;
	}

	public List<Integer> getValues() {
		return values;
	}
}
