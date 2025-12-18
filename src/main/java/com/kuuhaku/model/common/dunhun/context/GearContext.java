package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Gear;

import java.util.List;
import java.util.function.Consumer;

public class GearContext extends EffectContext {
	private final Gear gear;
	private final Actor<?> actor;
	private final List<Integer> values;

	public GearContext(Gear gear) {
		this(gear, List.of());
	}

	public GearContext(Gear gear, List<Integer> values) {
		super(gear.getOwner() != null ? gear.getOwner().getGame() : null);
		this.gear = gear;
		this.actor = gear.getOwner();
		this.values = values;
	}

	public Gear getGear() {
		return gear;
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
