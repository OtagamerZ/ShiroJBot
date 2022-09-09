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
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.DropCondition;
import com.kuuhaku.model.records.DropContent;
import com.kuuhaku.util.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Drop<T> {
	private static final RandomList<DropCondition<?>> pool = new RandomList<>() {{
		add(new DropCondition<>("low_cash",
				(rng) -> new Integer[]{
						DAO.queryNative(Integer.class, "SELECT AVG(balance) FROM account")
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					return acc.getBalance() <= 1000 * avg * rng.nextDouble();
				}
		));
		add(new DropCondition<>("high_cash",
				(rng) -> new Integer[]{
						DAO.queryNative(Integer.class, "SELECT AVG(balance) FROM account")
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					return acc.getBalance() >= 2500 + avg * 1.5 * rng.nextDouble();
				}
		));
		add(new DropCondition<>("level",
				(rng) -> new Integer[]{
						DAO.queryNative(Integer.class, "SELECT AVG(SQRT(xp / 100)) FROM profile")
				},
				(rng, vals, acc) -> {
					int avg = (int) vals[0];
					return acc.getHighestLevel() >= avg * rng.nextDouble();
				}
		));
		add(new DropCondition<>("cards",
				(rng) -> new Integer[]{
						DAO.queryNative(Integer.class, """
								SELECT AVG(x.count)
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
					return acc.getHighestLevel() >= avg / 10d + avg * rng.nextDouble();
				}
		));
		add(new DropCondition<>("cards_anime",
				(rng) -> {
					List<Anime> animes = DAO.queryAll(Anime.class, "SELECT a FROM Anime a WHERE visible = TRUE");
					return new Anime[]{Utils.getRandomEntry(rng, animes)};
				},
				(rng, vals, acc) -> {
					int avg = DAO.queryNative(Integer.class, """
							SELECT AVG(x.count)
							FROM (
							     SELECT COUNT(1) AS count
							     FROM kawaipon_card kc
							              INNER JOIN card c ON kc.card_id = c.id
							     WHERE kc.stash_entry IS NULL
							       AND c.anime_id = ?1
							     GROUP BY kc.kawaipon_uid
							     ) AS x
							""", ((Anime) Utils.getRandomEntry(rng, vals[0])).getId());

					return acc.getHighestLevel() >= avg / 10d + avg * rng.nextDouble();
				}
		));
	}};

	private final long seed = Constants.DEFAULT_RNG.nextLong();
	private final Rarity rarity = Utils.getRandomEntry(Rarity.getActualRarities());
	private final List<DropCondition<?>> conditions = Arrays.asList(new DropCondition<>[getConditionCount()]);
	private final Random rng = new Random(seed);
	private final String captcha = Utils.generateRandomHash(5);

	private final I18N locale;
	private final DropContent<T> content;
	private final BiConsumer<Integer, Account> applier;

	public Drop(I18N locale, Function<Integer, DropContent<T>> content, BiConsumer<Integer, Account> applier) {
		conditions.replaceAll(c -> pool.get());
		this.locale = locale;
		this.content = content.apply(rarity.getIndex());
		this.applier = applier;
	}

	public final Rarity getRarity() {
		return rarity;
	}

	public final int getConditionCount() {
		return (int) Math.ceil(rarity.getIndex() / 2f);
	}

	public final List<DropCondition<?>> getConditions() {
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

	public final boolean award(Account acc) {
		AtomicInteger seed = new AtomicInteger();
		boolean valid = conditions.stream().allMatch(dc -> dc.condition().apply(
				getRng(seed.getAndIncrement()),
				dc.extractor().apply(getRng()),
				acc
		));

		if (valid) {
			applier.accept(rarity.getIndex(), acc);
		}

		return valid;
	}

	private Random getRng() {
		rng.setSeed(seed);
		return rng;
	}

	private Random getRng(int salt) {
		rng.setSeed(seed + salt);
		return rng;
	}

	@Override
	public final String toString() {
		AtomicInteger seed = new AtomicInteger();
		return locale.get("str/drop_content", locale.get(content.key(), content.value())) + "\n\n" +
				locale.get("str/drop_requirements") + "\n" +
				conditions.stream()
						.map(dc -> locale.get(dc.key(), (Object[]) dc.extractor().apply(getRng(seed.getAndIncrement()))))
						.collect(Collectors.joining("\n"));
	}
}
