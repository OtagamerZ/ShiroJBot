package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.shoukan.Trigger;

public record CombatContext(Trigger trigger, Actor source, Actor target) {
}
