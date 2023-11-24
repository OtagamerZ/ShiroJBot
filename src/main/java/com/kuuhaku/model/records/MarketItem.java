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

import com.kuuhaku.model.common.Market;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.Utils;

import java.util.Calendar;

public record MarketItem(I18N locale, Market market, StashedCard sc) {

	@Override
	public String toString() {
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

		int sale;
		StashedCard offer = market.getDailyOffer();
		if (offer != null) {
			sale = offer.getId();
		} else {
			sale = -1;
		}

		double mult = 1;
		Calendar cal = Calendar.getInstance();

		if (cal.get(Calendar.MONTH) == Calendar.NOVEMBER && cal.get(Calendar.WEEK_OF_MONTH) == 4) {
			mult *= 0.66;
		}

		if (sale == sc.getId()) {
			mult *= 0.8;
		}

		Account seller = sc.getKawaipon().getAccount();
		String price = mult != 1
				? locale.get("str/offer_sale", sc.getPrice(), (int) (sc.getPrice() * mult), seller.getName() + " (<@" + seller.getUid() + ">)")
				: locale.get("str/offer", sc.getPrice(), seller.getName() + " (<@" + seller.getUid() + ">)");

		return new FieldMimic(
				"`ID: " + sc.getId() + "` " + sc,
				sc.getCard().getRarity().getEmote(sc.getCard()) + rarity + quality +
				"\n" + sc.getCard().getAnime().toString() +
				"\n" + price
		).toString();
	}
}
