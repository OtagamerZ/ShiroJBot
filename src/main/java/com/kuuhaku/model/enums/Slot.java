/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.enums;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Slot {
	JACKPOT(
			"<:7_s:680405919057969155>", "Impossível, acabaram de detonar a loteria. **JACKPOT**!!!",
			0, 150
	),
	DIAMOND(
			"<:diamond_s:680405919317753877>", "Assalto ao banco da sorte, saque de diamantes!",
			50, 80
	),
	HORSESHOE(
			"<:horseshoe_s:680405919213158525>", "Alguém sequestrou um duende, baú de ferraduras de ouro!",
			24, 65
	),
	BAR(
			"<:bar_s:680405918667898910>", "Chamem a polícia, temos um sortudo!",
			15, 45
	),
	BELL(
			"<:bell_s:680405919732990017>", "Toquem os sinos!",
			10, 16
	),
	HEART(
			"<:heart_s:680405919183405086>", "O amor está no ar!",
			5.75, 6
	),
	CHERRY(
			"<:cherry_s:680448442832912419>", "Cerejas para o bolo!",
			2.2, 4
	),
	WATERMELON(
			"<:watermelon_s:680405919548440587>", "Festa das melancias!",
			1.5, 2
	),
	LEMON(
			"<:lemon_s:680405919901024284>", "Eita, parece que você está azedo hoje!",
			0.8, 1
	);

	private final String emote;
	private final String message;
	private final double multiplier;
	private final int influence;

	Slot(String emote, String message, double multiplier, int influence) {
		this.emote = emote;
		this.message = message;
		this.multiplier = multiplier;
		this.influence = influence;
	}

	public String getEmote() {
		return emote;
	}

	public String getMessage() {
		return message;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public int getInfluence() {
		return influence;
	}

	@Override
	public String toString() {
		return emote;
	}

	public static Slot getCombo(List<Slot> rolled) {
		Slot out = null;
		Set<Slot> dist = new HashSet<>(rolled);
		for (Slot slot : dist) {
			if (Collections.frequency(rolled, slot) == 3) {
				out = slot;
				break;
			}
		}

		return out;
	}
}
