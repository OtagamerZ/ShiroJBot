package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.common.dunhun.MonsterBase;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.Skill;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public record CombatContext(
		Combat combat,
		Trigger trigger,
		Actor<?> source,
		AtomicReference<Actor<?>> target,
		Usable usable,
		AtomicInteger value,
		Set<String> tags
) {
	public CombatContext(Combat combat, Trigger trigger) {
		this(combat, trigger, null, (Actor<?>) null, null, null);
	}

	public CombatContext(Combat combat, Trigger trigger, Actor<?> source, Actor<?> target, Usable usable, AtomicInteger value) {
		this(combat, trigger, source, new AtomicReference<>(target), usable, value);
	}

	public CombatContext(Combat combat, Trigger trigger, Actor<?> source, AtomicReference<Actor<?>> target, Usable usable, AtomicInteger value) {
		this(combat, trigger, source, target, usable, value, new HashSet<>());

		if (usable instanceof Skill s) {
			for (Object tag : s.getStats().getTags()) {
				tags.add(tag.toString());
			}
		}

		if (source instanceof MonsterBase<?> m) {
			for (Object tag : m.getStats().getTags()) {
				tags.add(tag.toString());
			}
		}
	}

	public boolean isEnemy() {
		return source.getTeam() != target.get().getTeam();
	}
}
