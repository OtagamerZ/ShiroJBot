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
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.DropCondition;
import com.kuuhaku.model.records.DropContent;
import com.kuuhaku.util.Utils;
import kotlin.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class Drop<T> {
	private static final RandomList<DropCondition> pool = new RandomList<>() {{
		add(new DropCondition("low_cash",
				(rng) -> new Object[]{
						DAO.queryNative(Integer.class, "SELECT GEO_MEAN(balance) FROM account WHERE balance > 0")
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					return acc.getBalance() <= 1000 * avg * rng.nextDouble();
				}
		), 2);
		add(new DropCondition("high_cash",
				(rng) -> new Object[]{
						DAO.queryNative(Integer.class, "SELECT GEO_MEAN(balance) FROM account WHERE balance > 0")
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					return acc.getBalance() >= 2500 + avg * 1.5 * rng.nextDouble();
				}
		), 2);
		add(new DropCondition("level",
				(rng) -> new Object[]{
						DAO.queryNative(Integer.class, "SELECT GEO_MEAN(SQRT(xp / 100)) FROM profile WHERE xp > 0")
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					return acc.getHighestLevel() >= (int) (avg * rng.nextDouble());
				}
		), 3);
		add(new DropCondition("cards",
				(rng) -> new Object[]{
						DAO.queryNative(Integer.class, """
								SELECT GEO_MEAN(x.count)
								FROM (
								     SELECT COUNT(1) AS count
								     FROM kawaipon_card
								     WHERE stash_entry IS NULL
								     GROUP BY kawaipon_uid
								     ) AS x
								""")
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					Pair<Integer, Integer> total = acc.getKawaipon().countCards();

					return total.getFirst() + total.getSecond() >= avg / 10d + avg * rng.nextDouble();
				}
		), 3);
		add(new DropCondition("cards_anime",
				(rng) -> {
					List<Anime> animes = DAO.queryAll(Anime.class, "SELECT a FROM Anime a WHERE visible = TRUE");
					Anime anime = Utils.getRandomEntry(rng, animes);

					return new Object[]{
							DAO.queryNative(Integer.class, """
							SELECT GEO_MEAN(x.count)
							FROM (
							     SELECT COUNT(1) AS count
							     FROM kawaipon_card kc
							              INNER JOIN card c ON kc.card_id = c.id
							     WHERE kc.stash_entry IS NULL
							       AND c.anime_id = ?1
							     GROUP BY kc.kawaipon_uid
							     ) AS x
							""", anime.getId()),
							anime
					};
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					Pair<Integer, Integer> total = acc.getKawaipon().countCards((Anime) vals[1]);

					return total.getFirst() + total.getSecond() >= avg / 10d + avg * rng.nextDouble();
				}
		), 1);
	}};

	private final long seed = Constants.DEFAULT_RNG.nextLong();
	private final Rarity rarity = Utils.getRandomEntry(Rarity.getActualRarities());
	private final List<DropCondition> conditions = Arrays.asList(new DropCondition[getConditionCount()]);
	private final Random rng = new Random(seed);
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
		System.out.println(conditions);

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
		AtomicInteger i = new AtomicInteger();
		return conditions.stream().allMatch(dc -> dc.condition().apply(
				getRng(i.incrementAndGet()),
				dc.extractor().apply(getRng(i.get())),
				acc
		));
	}

	public final void award(Account acc) {
		if (check(acc)) {
			applier.accept(rarity.getIndex(), acc);
		}
	}

	public Random getRng() {
		rng.setSeed(seed);
		return rng;
	}

	public Random getRng(int index) {
		rng.setSeed(seed + index);
		return rng;
	}
}
