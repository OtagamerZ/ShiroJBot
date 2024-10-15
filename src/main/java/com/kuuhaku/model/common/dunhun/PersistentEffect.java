package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;

import java.util.function.BiConsumer;

public class PersistentEffect extends EffectBase {
	public PersistentEffect(Actor owner, int duration, BiConsumer<EffectBase, Actor> effect) {
		super(owner, duration, effect);
	}
}
