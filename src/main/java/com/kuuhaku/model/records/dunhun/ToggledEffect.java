package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.common.dunhun.Actor;

import java.util.function.Consumer;

public record ToggledEffect(int reservation, Consumer<Actor<?>> onEnable, Consumer<Actor<?>> onDisable) {
}
