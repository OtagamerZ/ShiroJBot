/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.util.Utils;
import org.intellij.lang.annotations.Language;

import java.util.Arrays;

public enum StashFilter {
	NAME("n", "AND c.card.id LIKE '%%'||?%s||'%%'"),
	RARITY("r", "AND cast(c.card.rarity AS STRING) LIKE '%%'||?%s||'%%'"),
	TIER("t", "AND e.tier = ?%s"),
	ANIME("a", "AND c.card.anime.id LIKE '%%'||?%s||'%%'"),
	CHROME("c", "AND cd.chrome = TRUE"),
	KAWAIPON("k", "AND c.type = 'KAWAIPON'"),
	SENSHI("s", "AND s.id IS NOT NULL"),
	EVOGEAR("e", "AND e.id IS NOT NULL"),
	FIELD("f", "AND f.id IS NOT NULL"),
	VALID("v", "AND c.deck IS NULL"),

	// Stash-only
	LOCKED("l", "AND c.locked = TRUE", false),

	// Market-only
	MIN("gt", "AND c.price >= ?%s", true),
	MAX("lt", "AND c.price <= ?%s", true),
	MINE("m", "AND c.kawaipon.uid = ?%s", true);

	@Language("JPAQL")
	public static final String BASE_QUERY = """
			SELECT c FROM StashedCard c
			INNER JOIN CardDetails cd ON cd.uuid = c.uuid
			LEFT JOIN KawaiponCard kc ON kc.uuid = c.uuid
			LEFT JOIN Senshi s ON s.card = c.card
			LEFT JOIN Evogear e ON e.card = c.card
			LEFT JOIN Field f ON f.card = c.card
			""";

	private final String shortName;
	private final String whereClause;
	private final Boolean market;

	StashFilter(String shortName, String whereClause) {
		this(shortName, whereClause, null);
	}

	StashFilter(String shortName, String whereClause, Boolean market) {
		this.shortName = shortName;
		this.whereClause = whereClause;
		this.market = market;
	}

	public String getLongName() {
		return name().toLowerCase();
	}

	public String getShortName() {
		return shortName;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public boolean isStashOnly() {
		return market != null && !market;
	}

	public boolean isMarketOnly() {
		return market != null && market;
	}

	public boolean hasParam() {
		return whereClause.contains("?%s");
	}

	public String toString(I18N locale) {
		return locale.get("search/" + name());
	}

	public static StashFilter getByArgument(String arg) {
		return Arrays.stream(values()).parallel()
				.filter(f -> Utils.equalsAny(arg, f.shortName, f.name()))
				.findFirst()
				.orElse(null);
	}
}
