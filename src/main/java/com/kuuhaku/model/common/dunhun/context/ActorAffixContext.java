package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Affix;

public class ActorAffixContext extends ActorContext {
	private Affix affix;

	public ActorAffixContext(Actor<?> actor, Affix affix) {
		super(actor);
		this.affix = affix;
	}

	public Affix getAffix() {
		return affix;
	}
}
