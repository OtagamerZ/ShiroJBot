package com.kuuhaku.handlers.games.tabletop.games.shoukan.records;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;

public record Target(Champion card, Side side, int index) {
	@Override
	public String toString() {
		return card.getName();
	}
}
