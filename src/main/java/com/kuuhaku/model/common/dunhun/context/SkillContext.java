package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.dunhun.Actor;
import com.ygimenez.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SkillContext extends EffectContext {
	private final List<Actor<?>> validTargets = new ArrayList<>();

	private final Actor<?> source;
	private final Actor<?> target;
	private final Usable usable;
	private final List<Integer> values;
	private final JSONObject vars;

	public SkillContext(Actor<?> source, Actor<?> target, Usable usable) {
		this(source, target, usable, List.of(), new JSONObject());
	}

	public SkillContext(Actor<?> source, Actor<?> target, Usable usable, List<Integer> values, JSONObject vars) {
		super(source.getGame());
		this.source = source;
		this.target = target;
		this.usable = usable;
		this.values = values;
		this.vars = vars;
	}

	public Actor<?> getSource() {
		return source;
	}

	public Actor<?> getTarget() {
		return target;
	}

	public Usable getUsable() {
		return usable;
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
