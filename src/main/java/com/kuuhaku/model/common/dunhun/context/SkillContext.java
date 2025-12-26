package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.dunhun.Actor;
import com.ygimenez.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SkillContext extends EffectContext<Usable> {
	private final List<Actor<?>> validTargets = new ArrayList<>();

	private final Actor<?> origin;
	private final Actor<?> target;
	private final List<Integer> values;
	private final JSONObject vars;

	public SkillContext(Actor<?> origin, Actor<?> target, Usable usable) {
		this(origin, target, usable, List.of(), new JSONObject());
	}

	public SkillContext(Actor<?> origin, Actor<?> target, Usable usable, List<Integer> values, JSONObject vars) {
		super(origin.getGame(), usable);
		this.origin = origin;
		this.target = target;
		this.values = values;
		this.vars = vars;
	}

	public Actor<?> getOrigin() {
		return origin;
	}

	public Actor<?> getTarget() {
		return target;
	}

	public List<Integer> getValues() {
		return values;
	}

	public JSONObject getVars() {
		return vars;
	}

	public List<Actor<?>> getValidTargets() {
		return validTargets;
	}
}
