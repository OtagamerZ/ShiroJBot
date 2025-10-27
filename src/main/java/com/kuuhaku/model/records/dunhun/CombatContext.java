package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;

import java.util.concurrent.atomic.AtomicInteger;

public record CombatContext(Trigger trigger, Actor<?> source, Actor<?> target, AtomicInteger value) {
	public boolean isEnemy() {
		return source.getTeam() != target.getTeam();
	}
}
