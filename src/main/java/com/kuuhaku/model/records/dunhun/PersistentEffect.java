package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public record PersistentEffect(Actor target, AtomicInteger duration, Consumer<Actor> effect) {
	public PersistentEffect(Actor target, int duration, Consumer<Actor> effect) {
		this(target, new AtomicInteger(duration), effect);
	}
}
