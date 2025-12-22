package com.kuuhaku.model.records.dunhun;

public record ToggledEffect(int reservation, Runnable onEnable, Runnable onDisable) {
}
