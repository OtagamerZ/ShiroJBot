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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.model.records.DropCondition;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import kotlin.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public abstract class Drop {
	private final long seed = ThreadLocalRandom.current().nextLong();
	private final String captcha = Utils.generateRandomHash(5);

	private final Rarity rarity;
	private final String content;
	private final BiConsumer<Integer, Account> applier;
	private final List<DropCondition> conditions;

	public Drop(Rarity rarity, Function<Integer, String> content, BiConsumer<Integer, Account> applier) {
		this.rarity = rarity;
		this.content = content.apply(rarity.getIndex());
		this.applier = applier;

		RandomList<DropCondition> pool = new RandomList<>();
		pool.add(new DropCondition("low_cash",
				(rng) -> {
					int avg = DAO.queryNative(Integer.class, "SELECT avg FROM v_balance");

					return new Object[]{(int) Calc.rng(avg, avg * 1.9, rng)};
				},
				(vals, acc) -> acc.getBalance() <= (int) vals[0]
		), 50);
		pool.add(new DropCondition("high_cash",
				(rng) -> {
					int avg = DAO.queryNative(Integer.class, "SELECT avg FROM v_balance");

					return new Object[]{(int) Calc.rng(avg * 0.1, avg, rng)};
				},
				(vals, acc) -> acc.getBalance() >= (int) vals[0]
		), 30);
		pool.add(new DropCondition("level_low",
				(rng) -> {
					int val = DAO.queryNative(Integer.class,  "SELECT highest_lvl FROM v_xp");

					return new Object[]{Calc.rng(val / 2, val, rng)};
				},
				(vals, acc) -> acc.getHighestLevel() <= (int) vals[0]
		), 50);
		pool.add(new DropCondition("level_high",
				(rng) -> {
					int val = DAO.queryNative(Integer.class, "SELECT highest_lvl FROM v_xp");

					return new Object[]{Calc.rng(1, val / 2, rng)};
				},
				(vals, acc) -> acc.getHighestLevel() >= (int) vals[0]
		), 30);
		pool.add(new DropCondition("cards",
				(rng) -> {
					int avg = DAO.queryNative(Integer.class, """
							SELECT round(geo_mean(x.count))
							FROM (
							     SELECT count(1) AS count
							     FROM kawaipon_card kc
							              LEFT JOIN stashed_card sc ON kc.uuid = sc.uuid
							     WHERE sc.id IS NULL
							     GROUP BY kc.kawaipon_uid
							     ) AS x
							WHERE x.count > 0
							""");

					return new Object[]{(int) Calc.rng(avg / 2d, (avg * 1.5), rng)};
				},
				(vals, acc) -> {
					Pair<Integer, Integer> total = acc.getKawaipon().countCards();

					return total.getFirst() + total.getSecond() >= (int) vals[0];
				}
		), 25);
		pool.add(new DropCondition("cards_anime",
				(rng) -> {
					List<Anime> animes = DAO.queryAll(Anime.class, "SELECT a FROM Anime a WHERE visible = TRUE");
					Anime anime = Utils.getRandomEntry(rng, animes);

					return new Object[]{Calc.rng(1, anime.getCount(), rng), anime};
				},
				(vals, acc) -> {
					Pair<Integer, Integer> total = acc.getKawaipon().countCards((Anime) vals[1]);

					return total.getFirst() + total.getSecond() >= (int) vals[0];
				}
		), 15);
		pool.add(new DropCondition("title",
				(rng) -> new Object[]{Utils.getRandomEntry(rng, DAO.findAll(Title.class))},
				(vals, acc) -> acc.hasTitle(((Title) vals[0]).getId())
		), 10);

		this.conditions = Arrays.asList(new DropCondition[getConditionCount()]);
		conditions.replaceAll(c -> pool.remove());
	}

	public final Rarity getRarity() {
		return rarity;
	}

	public final int getConditionCount() {
		return (int) Math.ceil(rarity.getIndex() / 2f);
	}

	public String getContent() {
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
			return String.join(Constants.VOID, captcha.split(""));
		}

		return captcha;
	}

	public final boolean check(Account acc) {
		RandomGenerator rng = getRng();

		for (DropCondition dc : conditions) {
			if (!dc.condition().apply(dc.extractor().apply(rng), acc)) {
				return false;
			}
		}

		return true;
	}

	public final RandomGenerator getRng() {
		return new SplittableRandom(seed);
	}

	public final void award(Account acc) {
		if (check(acc)) {
			applier.accept(rarity.getIndex(), acc);
		}
	}
}
