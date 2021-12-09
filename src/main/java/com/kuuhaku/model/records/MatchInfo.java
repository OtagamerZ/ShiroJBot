package com.kuuhaku.model.records;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;

public record MatchInfo(String id, Side side, boolean winner, double manaEff, double damageEff, double sustainEff) {
}
