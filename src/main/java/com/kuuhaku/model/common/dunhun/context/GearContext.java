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

	public GearContext(Gear gear, GearAffix affix) {
		this(gear, affix, null, List.of());
	}

	public GearContext(Gear gear, GearAffix affix, Actor<?> owner, List<Integer> values) {
		super(owner != null ? owner.getGame() : null, gear);
		this.affix = affix;
		this.actor = owner;
		this.values = values;
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
