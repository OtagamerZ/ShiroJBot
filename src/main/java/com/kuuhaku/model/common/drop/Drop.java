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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.DropCondition;
import com.kuuhaku.model.records.DropContent;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import kotlin.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class Drop<T> {
	private final RandomList<DropCondition> pool = new RandomList<>() {{
		add(new DropCondition("low_cash",
				(rng) -> {
					int avg = DAO.queryNative(Integer.class, "SELECT GEO_MEAN(balance) FROM account WHERE balance > 0");

					return new Object[]{(int) Calc.rng(avg, avg * 1.9, rng)};
				},
				(vals, acc) -> acc.getBalance() <= (int) vals[0]
		), 2);
		add(new DropCondition("high_cash",
				(rng) -> {
					int avg = DAO.queryNative(Integer.class, "SELECT GEO_MEAN(balance) FROM account WHERE balance > 0");

					return new Object[]{(int) Calc.rng(avg * 0.1, avg, rng)};
				},
				(vals, acc) -> acc.getBalance() >= (int) vals[0]
		), 2);
		add(new DropCondition("level",
				(rng) -> {
					int avg = DAO.queryNative(Integer.class, "SELECT GEO_MEAN(SQRT(xp / 100)) FROM profile WHERE xp > 100");

					return new Object[]{(int) Calc.rng(avg / 2d, (avg * 1.5), rng)};
				},
				(vals, acc) -> acc.getHighestLevel() >= (int) vals[0]
		), 3);
		add(new DropCondition("cards",
				(rng) -> {
					int avg = DAO.queryNative(Integer.class, """
							SELECT GEO_MEAN(x.count)
							FROM (
							     SELECT COUNT(1) AS count
							     FROM kawaipon_card
							     WHERE stash_entry IS NULL
							     GROUP BY kawaipon_uid
							     ) AS x
							""");

					return new Object[]{(int) Calc.rng(avg / 2d, (avg * 1.5), rng)};
				},
				(vals, acc) -> {
					Pair<Integer, Integer> total = acc.getKawaipon().countCards();

					return total.getFirst() + total.getSecond() >= (int) vals[0];
				}
		), 3);
		add(new DropCondition("cards_anime",
				(rng) -> {
					List<Anime> animes = DAO.queryAll(Anime.class, "SELECT a FROM Anime a WHERE visible = TRUE");
					Anime anime = Utils.getRandomEntry(rng, animes);

					int avg = DAO.queryNative(Integer.class, """
									SELECT GEO_MEAN(x.count)
									FROM (
									     SELECT COUNT(1) AS count
									     FROM kawaipon_card kc
									              INNER JOIN card c ON kc.card_id = c.id
									     WHERE kc.stash_entry IS NULL
									       AND c.anime_id = ?1
									     GROUP BY kc.kawaipon_uid
									     ) AS x
									""", anime.getId());

					return new Object[]{(int) Calc.rng(avg / 2d, Math.min(avg * 1.5, anime.getCount()), seed), anime};
				},
				(vals, acc) -> {
					Pair<Integer, Integer> total = acc.getKawaipon().countCards((Anime) vals[1]);

					return total.getFirst() + total.getSecond() >= (int) vals[0];
				}
		), 1);
	}};

	private final long seed = ThreadLocalRandom.current().nextLong();
	private final Rarity rarity = Utils.getRandomEntry(Rarity.getActualRarities());
	private final List<DropCondition> conditions = Arrays.asList(new DropCondition[getConditionCount()]);
	private final String captcha = Utils.generateRandomHash(5);

	private final DropContent<T> content;
	private final BiConsumer<Integer, Account> applier;

	public Drop(Function<Integer, DropContent<T>> content, BiConsumer<Integer, Account> applier) {
		conditions.replaceAll(c -> pool.remove());

		this.content = content.apply(rarity.getIndex());
		this.applier = applier;
	}

	public final Rarity getRarity() {
		return rarity;
	}

	public final int getConditionCount() {
		return (int) Math.ceil(rarity.getIndex() / 2f);
	}

	public DropContent<T> getContent() {
		return content;
	}

	public final List<DropCondition> getConditions() {
		return conditions;
	}

	public final long getSeed() {
		return seed;
	}

	public final String getCaptcha(boolean krangle) {
		if (krangle) {
			return String.join(Constants.VOID, captcha.split("\\."));
		}

		return captcha;
	}

	public final boolean check(Account acc) {
		Random rng = getRng();

		for (DropCondition dc : conditions) {
			if (!dc.condition().apply(dc.extractor().apply(rng), acc)) {
				return false;
			}
		}

		return true;
	}

	public final Random getRng() {
		return new Random(seed);
	}

	public final void award(Account acc) {
		if (check(acc)) {
			applier.accept(rarity.getIndex(), acc);
		}
	}
}
