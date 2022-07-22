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

package com.kuuhaku.model.common;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import org.apache.commons.cli.Option;

import java.util.*;

public class Market {
	private final String uid;
	private final Map<String, String> FILTERS = new LinkedHashMap<>() {{
		put("n", "AND c.card.id LIKE '%'||?1||'%'");
		put("r", "AND CAST(c.card.rarity AS STRING) LIKE '%'||?2||'%'");
		put("a", "AND c.card.anime.id LIKE '%'||?3||'%'");
		put("c", "AND c.chrome = TRUE");
		put("k", "AND c.type = 'KAWAIPON'");
		put("e", "AND c.type = 'EVOGEAR'");
		put("f", "AND c.type = 'FIELD'");
		put("v", "AND c.deck IS NULL");
		put("gl", "AND c.price >= ?4");
		put("lt", "AND c.price <= ?5");
		put("m", "AND c.kawaipon.uid = ?6");
	}};

	public Market(String uid) {
		this.uid = uid;
	}

	public List<StashedCard> getOffers(Option[] opts) {
		List<Object> params = new ArrayList<>();
		XStringBuilder query = new XStringBuilder("SELECT c FROM StashedCard c WHERE c.price > 0");

		for (Option opt : opts) {
			query.appendNewLine(FILTERS.get(opt.getOpt()));

			if (opt.hasArg()) {
				params.add(opt.getValue().toUpperCase(Locale.ROOT));
			}
		}

		query.appendNewLine("ORDER BY c.card.rarity, c.price, c.card.id");

		return DAO.queryAll(StashedCard.class, query.toString(), params.toArray());
	}

	public boolean buy(int id) {
		StashedCard sc = DAO.find(StashedCard.class, id);
		if (sc == null) return false;

		DAO.apply(Account.class, uid, a -> {
			a.consumeCR(sc.getPrice(), "Purchased " + sc);
			sc.setKawaipon(a.getKawaipon());
			sc.setPrice(0);
			sc.save();
		});
		DAO.apply(Account.class, sc.getKawaipon().getUid(), a -> {
			a.addCR(sc.getPrice(), "Sold " + sc);
			a.getUser().openPrivateChannel()
					.flatMap(c -> c.sendMessage(a.getEstimateLocale().get("success/market_notification", sc, sc.getPrice())))
					.queue(null, Utils::doNothing);
		});

		return true;
	}
}
