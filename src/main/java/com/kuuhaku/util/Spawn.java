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

package com.kuuhaku.util;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.FixedSizeDeque;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.SingleUseReference;
import com.kuuhaku.model.common.drop.CreditDrop;
import com.kuuhaku.model.common.drop.Drop;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.records.GuildBuff;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.jodah.expiringmap.ExpiringMap;
import org.shredzone.commons.suncalc.MoonIllumination;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class Spawn {
	private static final ExpiringMap<String, SingleUseReference<KawaiponCard>> spawnedCards = ExpiringMap.builder()
			.variableExpiration()
			.build();
	private static final ExpiringMap<String, SingleUseReference<Drop<?>>> spawnedDrops = ExpiringMap.builder()
			.variableExpiration()
			.build();

	private static Pair<Integer, MoonIllumination> illum = null;

	private static FixedSizeDeque<Anime> lastAnimes = new FixedSizeDeque<>(3);
	private static FixedSizeDeque<Card> lastCards = new FixedSizeDeque<>(15);

	public synchronized static KawaiponCard getKawaipon(GuildBuff gb, GuildMessageChannel channel, User u) {
		if (spawnedCards.containsKey(channel.getId())) return null;

		double dropRate = 8 * (1.2 * Math.pow(Math.E, -0.001 * Math.min(channel.getGuild().getMemberCount(), 1000))) * (1 + gb.card()) * getQuantityMult();
		double rarityBonus = 1 * (1 + gb.rarity()) * getRarityMult();

		KawaiponCard card = null;
		if (Calc.chance(dropRate)) {
			RandomList<Rarity> rarities = new RandomList<>(2 * rarityBonus);
			for (Rarity r : Rarity.getActualRarities()) {
				rarities.add(r, DAO.queryNative(Integer.class, "SELECT get_weight('KAWAIPON', ?1)", 6 - r.getIndex()));
			}

			Rarity rarity = rarities.get();
			List<Anime> animes = DAO.queryAll(Anime.class, """
							SELECT c.anime
							FROM Card c
							WHERE c.anime.visible = TRUE
							AND (?1 = 0 OR c.anime.id NOT IN ?2)
							AND (?3 = 0 OR c.id NOT IN ?4)
							AND c.rarity = ?5
							""",
					lastAnimes.size(), lastAnimes.stream().map(Anime::getId).toList(),
					lastCards.size(), lastCards.stream().map(Card::getId).toList(),
					rarity
			);

			Anime anime;
			if (animes.isEmpty()) {
				anime = lastAnimes.removeFirst();
			} else {
				anime = Utils.getRandomEntry(animes);
			}
			lastAnimes.add(anime);

			RandomList<Card> cards = new RandomList<>(2 * rarityBonus);
			for (Card c : anime.getCards()) {
				if (c.getRarity().getIndex() <= 0) continue;

				cards.add(c, DAO.queryNative(Integer.class, "SELECT get_weight(?1, ?2)", c.getId(), u.getId()));
			}

			Card chosen = cards.get();
			lastCards.add(chosen);

			card = new KawaiponCard(chosen, Calc.chance(0.1 * rarityBonus));
			spawnedCards.put(
					channel.getId(),
					new SingleUseReference<>(card),
					(long) (60 / getQuantityMult()), TimeUnit.SECONDS
			);
		}

		return card;
	}

	public synchronized static Drop<?> getDrop(GuildBuff gb, GuildMessageChannel channel, User u) {
		if (spawnedDrops.containsKey(channel.getId())) return null;

		double dropRate = 10 * (1.2 * Math.pow(Math.E, -0.001 * Math.min(channel.getGuild().getMemberCount(), 1000))) * (1 + gb.drop()) * getQuantityMult();
		double rarityBonus = 1 * (1 + gb.rarity()) * getRarityMult();

		Drop<?> drop = null;
		if (Calc.chance(dropRate)) {
			RandomList<Drop<?>> rPool = new RandomList<>(1.75 * rarityBonus);
			rPool.add(new CreditDrop());

			drop = rPool.get();
			spawnedDrops.put(
					channel.getId(),
					new SingleUseReference<>(drop),
					(long) (60 / getQuantityMult()), TimeUnit.SECONDS
			);
		}

		return drop;
	}

	public static SingleUseReference<KawaiponCard> getSpawnedCard(GuildMessageChannel channel) {
		return spawnedCards.getOrDefault(channel.getId(), new SingleUseReference<>(null));
	}

	public static SingleUseReference<Drop<?>> getSpawnedDrop(GuildMessageChannel channel) {
		return spawnedDrops.getOrDefault(channel.getId(), new SingleUseReference<>(null));
	}

	public static Pair<Integer, MoonIllumination> getIllumination() {
		LocalDate now = LocalDate.now();
		if (illum == null || illum.getFirst() != now.getDayOfYear()) {
			MoonIllumination mi = MoonIllumination.compute().midnight().execute();
			illum = new Pair<>(now.getDayOfYear(), mi);
		}

		return illum;
	}

	/*
	NEW MOON = +50% quantity
	 */
	public static double getQuantityMult() {
		return 1 + (0.5 - Math.abs(getIllumination().getSecond().getPhase()) / 360);
	}

	/*
	FULL MOON = +50% rarity
	 */
	public static double getRarityMult() {
		return 1 + Math.abs(getIllumination().getSecond().getPhase()) / 360;
	}
}
