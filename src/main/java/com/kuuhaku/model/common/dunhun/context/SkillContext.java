package com.kuuhaku.model.common.dunhun.context;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.ygimenez.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SkillContext extends EffectContext {
	private final Actor<?> source;
	private final Actor<?> target;
	private final List<Integer> values;
	private final JSONObject vars;

	public SkillContext(Actor<?> source, Actor<?> target) {
		this(source, target, List.of(), new JSONObject());
	}

	public SkillContext(Actor<?> source, Actor<?> target, List<Integer> values, JSONObject vars) {
		super(source.getGame());
		this.source = source;
		this.target = target;
		this.values = values;
		this.vars = vars;
	}

	public Actor<?> getSource() {
		return source;
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
}
