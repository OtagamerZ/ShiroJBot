package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;

import java.util.concurrent.atomic.AtomicInteger;

public record CombatContext(Trigger trigger, Actor<?> source, Actor<?> target, Usable usable, AtomicInteger value) {
	public boolean isEnemy() {
		return source.getTeam() != target.getTeam();
	}
}
