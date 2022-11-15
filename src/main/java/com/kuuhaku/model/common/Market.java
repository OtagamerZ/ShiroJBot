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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.shiro.GlobalProperty;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.API;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.cli.Option;
import org.apache.http.client.methods.HttpHead;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Market {
	private static final Dimension BANNER_SIZE = new Dimension(450, 100);
	private final String uid;
	private final Map<String, String> FILTERS = new LinkedHashMap<>() {{
		put("n", "AND c.card.id LIKE '%%'||?%s||'%%'");
		put("r", "AND CAST(c.card.rarity AS STRING) LIKE '%%'||?%s||'%%'");
		put("a", "AND c.card.anime.id LIKE '%%'||?%s||'%%'");
		put("c", "AND c.chrome = TRUE");
		put("k", "AND c.type = 'KAWAIPON'");
		put("e", "AND c.type = 'EVOGEAR'");
		put("f", "AND c.type = 'FIELD'");
		put("gl", "AND c.price >= ?%s");
		put("lt", "AND c.price <= ?%s");
		put("m", "AND c.kawaipon.uid = ?%s");
	}};

	public Market(String uid) {
		this.uid = uid;
	}

	public List<StashedCard> getOffers(Option[] opts, int page) {
		List<Object> params = new ArrayList<>();
		XStringBuilder query = new XStringBuilder("""
				SELECT c FROM StashedCard c
				LEFT JOIN KawaiponCard kc ON kc.uuid = c.uuid
				LEFT JOIN Evogear e ON e.card = c.card
				WHERE c.price > 0
				""");

		AtomicInteger i = new AtomicInteger(1);
		for (Option opt : opts) {
			String filter = FILTERS.get(opt.getOpt());
			if (filter.contains("%s")) {
				filter = filter.formatted(i.getAndIncrement());
			}

			query.appendNewLine(filter);

			if (opt.hasArg()) {
				if (opt.getOpt().equals("m")) {
					params.add(uid);
				} else {
					params.add(opt.getValue().toUpperCase());
				}
			}
		}

		query.appendNewLine("""
				ORDER BY c.price / COALESCE(
						e.tier,
					   	CASE c.card.rarity
					   		WHEN 'COMMON' THEN 1
				      		WHEN 'UNCOMMON' THEN 1.5
				      		WHEN 'RARE' THEN 2
				      		WHEN 'ULTRA_RARE' THEN 2.5
				      		WHEN 'LEGENDARY' THEN 3
				      		ELSE 1
				       	END)
				       	, c.type
					   	, c.card.id
				""");

		return DAO.queryBuilder(
				StashedCard.class,
				query.toString(),
				q -> q.setFirstResult(page * 10).setMaxResults(10).getResultList(),
				params.toArray()
		);
	}

	public boolean buy(int id) {
		StashedCard sc = DAO.find(StashedCard.class, id);
		if (sc == null) return false;

		GlobalProperty gp = Utils.getOr(DAO.find(GlobalProperty.class, "daily_offer"), new GlobalProperty("daily_offer", "{}"));
		int price = sc.getPrice();

		Account seller = sc.getKawaipon().getAccount();
		seller.addCR(price, "Sold " + sc);
		seller.getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage(seller.getEstimateLocale().get("success/market_notification", sc, price)))
				.queue(null, Utils::doNothing);

		Account buyer = DAO.find(Account.class, uid);
		try {
			int sale = new JSONObject(gp.getValue()).getInt("id");
			if (sale == sc.getId()) {
				buyer.consumeCR((long) (price * 0.8), "Purchased " + sc + " (SALE)");
			}
		} catch (Exception e) {
			buyer.consumeCR(price, "Purchased " + sc);
		}

		sc.setKawaipon(buyer.getKawaipon());
		sc.setPrice(0);
		sc.save();

		return true;
	}

	public StashedCard getDailyOffer() {
		API.call(new HttpHead(Constants.API_ROOT + "market/refresh"), null, null, null);

		GlobalProperty gp = DAO.find(GlobalProperty.class, "daily_offer");
		if (gp != null) {
			int id = new JSONObject(gp.getValue()).getInt("id");
			return DAO.query(StashedCard.class, "SELECT c FROM StashedCard c WHERE c.id = ?1 AND c.price > 0", id);
		}

		return null;
	}
}
