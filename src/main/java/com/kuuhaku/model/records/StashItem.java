/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records;

import com.kuuhaku.model.common.Trade;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.Utils;

public record StashItem(I18N locale, StashedCard sc) {

	@Override
	public String toString() {
		String uid = sc.getKawaipon().getUid();

		Trade t = Trade.getPending().get(uid);
		String location = "";
		if (t != null && t.getSelfOffers(uid).contains(sc.getId())) {
			location = " (" + locale.get("str/trade") + ")";
		} else if (sc.getDeck() != null) {
			location = " (" + locale.get("str/deck", sc.getDeck().getIndex()) + ")";
		} else if (sc.getPrice() > 0) {
			location = " (" + locale.get("str/market", sc.getPrice()) + ")";
		} else if (sc.isLock()) {
			location = " \uD83D\uDDD1️";
		}

		String rarity = locale.get("type/" + sc.getType());
		if (Utils.equalsAny(sc.getType(), CardType.KAWAIPON, CardType.SENSHI)) {
			rarity += " " + locale.get("rarity/" + sc.getCard().getRarity());
		} else if (sc.getType() == CardType.FIELD) {
			Field fd = sc.getCard().asField();
			rarity += switch (fd.getType()) {
				case NONE -> "";
				case DAY -> ":sunny:";
				case NIGHT -> ":crescent_moon:";
				case DUNGEON -> ":japanese_castle:";
			};
		}

		String quality = "";
		KawaiponCard kc = sc.getKawaiponCard();
		if (kc != null && kc.getQuality() > 0) {
			quality = " (Q: " + Utils.roundToString(kc.getQuality(), 1) + "%)";
		}

		return new FieldMimic(
				sc + location + (sc.isAccountBound() ? " \uD83D\uDD12" : ""),
				sc.getCard().getRarity().getEmote(sc.getCard()) + rarity + quality +
				"\n" + sc.getCard().getAnime().toString()
		).toString();
	}

	public String toString(int id) {
		return "`" + id + "` " + this;
	}
}
