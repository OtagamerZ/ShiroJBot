package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Affix;

public class ActorAffixContext extends ActorContext {
	private final Affix affix;
	private final int duration;

	public ActorAffixContext(Actor<?> actor, Affix affix) {
		this(actor, affix, -1);
	}

	public ActorAffixContext(Actor<?> actor, Affix affix, int duration) {
		super(actor);
		this.affix = affix;
		this.duration = duration;
	}

	public Affix getAffix() {
		return affix;
	}

	public int getDuration() {
		return duration;
	}
}
