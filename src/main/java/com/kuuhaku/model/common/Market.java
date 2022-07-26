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
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.GlobalProperty;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import com.kuuhaku.util.json.JSONArray;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.cli.Option;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

	public List<StashedCard> getOffers(Option[] opts) {
		List<Object> params = new ArrayList<>();
		XStringBuilder query = new XStringBuilder("""
				SELECT c FROM StashedCard c
				LEFT JOIN KawaiponCard kc ON kc.stashEntry = c
				LEFT JOIN Evogear e ON e.card = c.card
				WHERE c.price > 0
				""");

		AtomicInteger i = new AtomicInteger(1);
		for (Option opt : opts) {
			query.appendNewLine(FILTERS.get(opt.getOpt()).formatted(i.getAndIncrement()));

			if (opt.hasArg()) {
				params.add(opt.getValue().toUpperCase(Locale.ROOT));
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
					   	, c.card.id
				""");

		return DAO.queryAll(StashedCard.class, query.toString(), params.toArray());
	}

	public boolean buy(int id) {
		StashedCard sc = DAO.find(StashedCard.class, id);
		if (sc == null) return false;

		int price = sc.getPrice();
		DAO.apply(Account.class, sc.getKawaipon().getUid(), a -> {
			a.addCR(price, "Sold " + sc);
			a.getUser().openPrivateChannel()
					.flatMap(c -> c.sendMessage(a.getEstimateLocale().get("success/market_notification", sc, price)))
					.queue(null, Utils::doNothing);
		});
		DAO.apply(Account.class, uid, a -> {
			a.consumeCR(price, "Purchased " + sc);
			sc.setKawaipon(a.getKawaipon());
			sc.setPrice(0);
			sc.save();
		});

		return true;
	}

	public int getDailyOffer() {
		int seed = LocalDate.now().get(ChronoField.EPOCH_DAY);

		GlobalProperty today = DAO.find(GlobalProperty.class, "daily_offer");
		JSONObject jo;
		if (today == null) {
			jo = new JSONObject(){{
				put("updated", seed);
				put("id", DAO.queryNative(Integer.class, "SELECT c.id FROM stashed_card c WHERE c.price > 0 ORDER BY RANDOM()"));
			}};

			new GlobalProperty("daily_offer", jo).save();
		} else {
			jo = new JSONObject(today.getValue());

			if (jo.getInt("updated") != seed) {
				jo.put("updated", seed);
				jo.put("id", DAO.queryNative(Integer.class, "SELECT c.id FROM stashed_card c WHERE c.price > 0 ORDER BY RANDOM()"));

				today.save();
			}
		}

		return jo.getInt("id");
	}

	public BufferedImage generateBanner(I18N locale) {
		StashedCard offer = DAO.query(StashedCard.class, "SELECT c FROM StashedCard c WHERE c.id = ?1 AND c.price > 0", getDailyOffer());
		if (offer == null) return null;

		BufferedImage img;
		if (offer.getType() == CardType.KAWAIPON) {
			img = offer.getCard().drawCardNoBorder(offer.getKawaiponCard().isChrome());
		} else {
			img = offer.getCard().drawCardNoBorder();
		}

		BufferedImage banner = new BufferedImage(BANNER_SIZE.width, BANNER_SIZE.height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = banner.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setClip(new RoundRectangle2D.Double(0, 0,
				BANNER_SIZE.width, BANNER_SIZE.height,
				BANNER_SIZE.width * 0.1, BANNER_SIZE.width * 0.1
		));

		double imgFac = 1.25;
		int offset = (int) (img.getWidth() / imgFac);
		g2d.drawImage(img, 0, -50, offset, (int) (img.getHeight() / imgFac), null);

		Polygon poly = Graph.makePoly(
				offset, 0,
				BANNER_SIZE.width, 0,
				BANNER_SIZE.width, BANNER_SIZE.height,
				offset - 20, BANNER_SIZE.height
		);

		Graph.applyTransformed(g2d, g1 -> {
			g1.setColor(Graph.getColor(img));
			g1.fill(poly);

			AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
			g1.setComposite(ac);

			double ratio = ((BANNER_SIZE.getWidth() - offset) / img.getWidth());
			g1.clip(poly);
			g1.drawImage(img, offset, BANNER_SIZE.height / 2 - (int) (img.getHeight() * ratio) / 2,
					BANNER_SIZE.width - offset, (int) (img.getHeight() * ratio), null
			);
		});

		Graph.applyTransformed(g2d, offset, 0, g1 -> {
			g1.setColor(Color.WHITE);
			g1.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 25));

			String str = locale.get("str/daily_offer");
			Graph.drawOutlinedString(g1, str,
					(BANNER_SIZE.width - offset) / 2 - g1.getFontMetrics().stringWidth(str) / 2, 25,
					3, Color.BLACK
			);

			AtomicBoolean nextBig = new AtomicBoolean(false);
			Graph.drawMultilineString(g1,
					"""
							ID: %s | %s
							~~%s~~ %s#₵R
							""".formatted(offer.getId(), offer, offer.getPrice(), (int) (offer.getPrice() * 0.8)),
					0, 50, BANNER_SIZE.width, 2,
					s -> {
						JSONArray groups = Utils.extractGroups(s, "~~(.+)~~");

						g1.setFont(Fonts.OPEN_SANS_EXTRABOLD.deriveFont(Font.BOLD, 15));
						if (!groups.isEmpty()) {
							s = groups.getString(0);

							g1.setFont(Fonts.DEFAULT.deriveFont(Font.PLAIN, 15,
									Map.of(TextAttribute.STRIKETHROUGH, true)
							));

							nextBig.set(true);
						} else if (nextBig.get()) {
							g1.setColor(Color.ORANGE);
							g1.setFont(Fonts.DEFAULT.deriveFont(Font.BOLD, 40));
							g1.translate(5, 20);
						}

						return s.replace("#", " ");
					},
					(s, x, y) -> Graph.drawOutlinedString(g1, s, x, y, 3, Color.BLACK)
			);
		});

		return banner;
	}
}
