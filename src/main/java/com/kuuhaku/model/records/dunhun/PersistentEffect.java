package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public record PersistentEffect(Actor target, AtomicInteger duration, BiConsumer<PersistentEffect, Actor> effect) {
	public PersistentEffect(Actor target, int duration, BiConsumer<PersistentEffect, Actor> effect) {
		this(target, new AtomicInteger(duration), effect);
	}
}
