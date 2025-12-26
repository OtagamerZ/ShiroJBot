package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Actor;

public class ActorContext extends EffectContext<Actor<?>> {
	public ActorContext(Actor<?> actor) {
		super(actor.getGame(), actor);
	}
}
