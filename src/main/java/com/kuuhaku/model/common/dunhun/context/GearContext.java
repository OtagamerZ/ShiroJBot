package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.GearAffix;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class GearContext extends EffectContext<Gear> {
	private final GearAffix affix;
	private final Actor<?> actor;
	private final List<Integer> values;
	private final boolean shoukan;

	public GearContext(Gear gear, GearAffix affix, boolean shoukan) {
		this(gear, affix, null, List.of(), shoukan);
	}

	public GearContext(Gear gear, GearAffix affix, Actor<?> owner, List<Integer> values, boolean shoukan) {
		super(owner != null ? owner.getGame() : null, gear);
		this.affix = affix;
		this.actor = owner;
		this.values = values;
		this.shoukan = shoukan;
	}

	public GearAffix getAffix() {
		return affix;
	}

	public void withActor(Consumer<Actor<?>> action) {
		if (actor != null) {
			action.accept(actor);
		}
	}

	public List<Integer> getValues() {
		return values;
	}

	public boolean isShoukan() {
		return shoukan;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		GearContext that = (GearContext) o;
		return Objects.equals(affix, that.affix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), affix);
	}
}
