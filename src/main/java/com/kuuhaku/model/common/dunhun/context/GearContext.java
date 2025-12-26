package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Gear;

import java.util.List;
import java.util.function.Consumer;

public class GearContext extends EffectContext<Gear> {
	private final Actor<?> actor;
	private final List<Integer> values;

	public GearContext(Gear gear) {
		this(gear, null, List.of());
	}

	public GearContext(Gear gear, Actor<?> owner, List<Integer> values) {
		super(owner != null ? owner.getGame() : null, gear);
		this.actor = owner;
		this.values = values;
	}

	public void withActor(Consumer<Actor<?>> action) {
		if (actor != null) {
			action.accept(actor);
		}
	}

	public List<Integer> getValues() {
		return values;
	}
}
