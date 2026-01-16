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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.CardFilter;
import com.kuuhaku.model.persistent.shiro.GlobalProperty;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.MarketOrder;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.API;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import org.apache.commons.cli.Option;
import org.apache.hc.client5.http.classic.methods.HttpHead;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public record Market(String uid) {
	public List<StashedCard> getOffers(Option[] opts, int page) {
		List<Object> params = new ArrayList<>();
		XStringBuilder query = new XStringBuilder(CardFilter.BASE_QUERY);
		query.appendNewLine("WHERE c.price > 0");

		AtomicInteger i = new AtomicInteger(1);
		for (Option opt : opts) {
			CardFilter sf = CardFilter.getByArgument(opt.getOpt());
			if (sf == null || sf.isStashOnly()) continue;

			String filter = sf.getWhereClause();
			if (opt.hasArg()) {
				filter = filter.formatted(i.getAndIncrement());

				if (opt.getOpt().equalsIgnoreCase("m")) {
					params.add(uid);
				} else {
					params.add(opt.getValue().toUpperCase());
				}
			}

			query.appendNewLine(filter);
		}

		query.appendNewLine("""
				ORDER BY c.price / COALESCE(
						e.tier,
					   	CASE c.card.rarity
					   		WHEN 'COMMON' THEN 1
				      		WHEN 'UNCOMMON' THEN 1.5
				      		WHEN 'RARE' THEN 2
				      		WHEN 'EPIC' THEN 2.5
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
		return buy(null, id);
	}

	public boolean buy(MarketOrder order, int id) {
		StashedCard sc = DAO.find(StashedCard.class, id);
		if (sc == null) return false;

		int price = sc.getPrice();

		Account seller = sc.getKawaipon().getAccount();
		seller.addCR(price, "Sold " + sc);
		seller.getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage(seller.getEstimateLocale().get("success/market_notification", sc, price)))
				.queue(null, Utils::doNothing);

		Account buyer = DAO.find(Account.class, uid);
		try {
			if (sc.getId() == getDailyOffer()) {
				buyer.consumeCR((long) (price * 0.8), "Purchased " + sc + " (SALE)");
			} else {
				buyer.consumeCR(price, "Purchased " + sc);
			}
		} catch (Exception e) {
			buyer.consumeCR(price, "Purchased " + sc);
		}

		if (order != null) {
			buyer.getUser().openPrivateChannel()
					.flatMap(c -> c.sendMessage(seller.getEstimateLocale().get("success/market_order_filled", sc, price)))
					.queue(null, Utils::doNothing);

			if (order.getId() > 0) {
				order.delete();
			}
		}

		sc.setKawaipon(buyer.getKawaipon());
		sc.setInCollection(false);
		sc.setPrice(0);
		sc.save();

		return true;
	}

	public int getDailyOffer() {
		API.call(new HttpHead(Constants.API_ROOT + "market/refresh"), null, null, null);

		GlobalProperty gp = DAO.find(GlobalProperty.class, "daily_offer");
		if (gp != null) {
			return new JSONObject(gp.getValue()).getInt("id");
		}

		return -1;
	}
}
