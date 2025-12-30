package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Affix;

import java.util.Objects;

public class ActorContext extends EffectContext<Actor<?>> {
	private final Affix affix;
	private final int duration;

	public ActorContext(Actor<?> actor, Affix affix) {
		this(actor, affix, -1);
	}

	public ActorContext(Actor<?> actor, Affix affix, int duration) {
		super(actor != null ? actor.getGame() : null, actor);
		this.affix = affix;
		this.duration = duration;
	}

	public Affix getAffix() {
		return affix;
	}

	public int getDuration() {
		return duration;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		ActorContext that = (ActorContext) o;
		return Objects.equals(affix, that.affix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), affix);
	}
}
