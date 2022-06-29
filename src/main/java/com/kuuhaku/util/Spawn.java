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
import net.dv8tion.jda.api.entities.Guild;
import net.jodah.expiringmap.ExpiringMap;
import org.shredzone.commons.suncalc.MoonIllumination;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class Spawn {
	private static Pair<Integer, MoonIllumination> illum = null;
	private static Map<String, SingleUseReference<KawaiponCard>> spawnedCards = ExpiringMap.builder()
			//.expiration(1, TimeUnit.MINUTES)  TODO Return
			.expiration(20, TimeUnit.SECONDS)
			.build();

	public synchronized static KawaiponCard getKawaipon(Guild guild) {
		if (spawnedCards.containsKey(guild.getId())) return null;

		GuildConfig config = DAO.find(GuildConfig.class, guild.getId());
		if (config.getSettings().getKawaiponChannels().isEmpty()) return null;

		LocalDate now = LocalDate.now();
		if (illum == null || illum.getFirst() != now.getDayOfYear()) {
			MoonIllumination mi = MoonIllumination.compute().midnight().execute();
			illum = new Pair<>(now.getDayOfYear(), mi);
		}

		// TODO Remove
		int DEBUG_MULT = 10;

		double fac = 0.5 - Math.abs(illum.getSecond().getPhase()) / 360;
		double dropRate = 5 * DEBUG_MULT * (1 - fac) + (0.5 * Math.pow(Math.E, -0.001 * guild.getMemberCount()));
		double rarityBonus = 1 + fac;

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

			card = new KawaiponCard(Utils.getRandomEntry(cPool.get(rPool.get())), Calc.chance(0.005 * rarityBonus));
			spawnedCards.put(guild.getId(), new SingleUseReference<>(card));
		}

		return card;
	}

	public static SingleUseReference<KawaiponCard> getSpawnedCard(Guild guild) {
		return spawnedCards.getOrDefault(guild.getId(), new SingleUseReference<>(null));
	}
}
