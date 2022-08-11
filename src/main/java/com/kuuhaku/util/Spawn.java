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

package com.kuuhaku.util;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.SingleUseReference;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.TextChannel;
import net.jodah.expiringmap.ExpiringMap;
import org.shredzone.commons.suncalc.MoonIllumination;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class Spawn {
	private static final Map<String, SingleUseReference<KawaiponCard>> spawnedCards = ExpiringMap.builder()
			//.expiration(1, TimeUnit.MINUTES)  TODO Return
			.expiration(20, TimeUnit.SECONDS)
			.build();
	private static Pair<Integer, MoonIllumination> illum = null;

	public synchronized static KawaiponCard getKawaipon(TextChannel channel) {
		if (spawnedCards.containsKey(channel.getId())) return null;

		GuildConfig config = DAO.find(GuildConfig.class, channel.getGuild().getId());
		if (config.getSettings().getKawaiponChannels().isEmpty()) return null;

		// TODO Remove
		int DEBUG_MULT = 10;

		double dropRate = 5 * DEBUG_MULT * (1 - getQuantityMult()) + (0.5 * Math.pow(Math.E, -0.001 * channel.getGuild().getMemberCount()));
		double rarityBonus = 1 + getRarityMult();

		KawaiponCard card = null;
		if (Calc.chance(dropRate)) {
			List<Anime> animes = DAO.queryAll(Anime.class, "SELECT a FROM Anime a WHERE a.visible = TRUE");
			Map<Rarity, Set<Card>> cPool = Utils.getRandomEntry(animes).getCards().stream()
					.collect(Collectors.groupingBy(Card::getRarity, Collectors.toSet()));

			RandomList<Rarity> rPool = new RandomList<>(3 - rarityBonus);
			for (Rarity r : cPool.keySet()) {
				if (r.getIndex() <= 0) continue;

				rPool.add(r, 6 - r.getIndex());
			}

			card = new KawaiponCard(Utils.getRandomEntry(cPool.get(rPool.get())), Calc.chance(0.1 * rarityBonus));
			spawnedCards.put(channel.getId(), new SingleUseReference<>(card));
		}

		return card;
	}

	public static SingleUseReference<KawaiponCard> getSpawnedCard(TextChannel channel) {
		return spawnedCards.getOrDefault(channel.getId(), new SingleUseReference<>(null));
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
		return 0.5 - Math.abs(getIllumination().getSecond().getPhase()) / 360;
	}

	/*
	FULL MOON = +50% rarity
	 */
	public static double getRarityMult() {
		return Math.abs(getIllumination().getSecond().getPhase()) / 360;
	}
}
