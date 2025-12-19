package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Actor;

public class ActorContext extends EffectContext {
	private final Actor<?> actor;

	public ActorContext(Actor<?> actor) {
		super(actor.getGame());
		this.actor = actor;
	}

	public Actor<?> getActor() {
		return actor;
	}
}
