/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.Market;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.Utils;
import org.apache.commons.lang3.StringUtils;

public record MarketItem(I18N locale, Market market, StashedCard sc) {

	@Override
	public String toString() {
		String rarity = locale.get("rarity/" + sc.getCard().getRarity());
		if (sc.getType() == CardType.EVOGEAR) {
			Evogear ev = DAO.find(Evogear.class, sc.getCard().getId());
			rarity += " " + StringUtils.repeat("★", ev.getTier());
		} else if (sc.getType() == CardType.FIELD) {
			Field fd = DAO.find(Field.class, sc.getCard().getId());
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
			quality = locale.get("str/quality", Utils.roundToString(kc.getQuality(), 1));
		}

		int sale;
		StashedCard offer = market.getDailyOffer();
		if (offer != null) {
			sale = offer.getId();
		} else {
			sale = -1;
		}
		Account seller = sc.getKawaipon().getAccount();
		String price = "\n" + (sale == sc.getId()
				? locale.get("str/offer_sale", sc.getPrice(), (int) (sc.getPrice() * 0.8), seller.getName() + " (<@" + seller.getUid() + ">)")
				: locale.get("str/offer", sc.getPrice(), seller.getName() + " (<@" + seller.getUid() + ">)")
		);

		return "**" + sc + " " + price + "**" +
				"\n" + sc.getCard().getRarity().getEmote() + locale.get("type/" + sc.getType()) +
				"\n" + rarity +
				"\n" + sc.getCard().getAnime().toString() +
				"\n" + quality;
	}
}
