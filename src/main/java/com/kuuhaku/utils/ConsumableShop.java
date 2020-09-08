/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.utils;

import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.Consumable;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Member;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConsumableShop {
	private static final Map<String, Consumable> available = new HashMap<>() {{
		put("xpboost", new Consumable("XP Booster",
				"Ganha 3500 de XP instantâneamente (valor reduzido se você ganhou XP em menos de 10 segundos)",
				5000,
				(mb, ch, ms) -> {
					Member m = MemberDAO.getMemberById(mb.getId() + mb.getGuild().getId());
					ch.sendMessage(mb.getAsMention() + " utilizou um boost de experiência e ganhou " + m.addXp(3500) + " XP.").queue();
					MemberDAO.updateMemberConfigs(m);
				}));
		put("spawncard", new Consumable("Invocar Carta",
				"Invoca uma carta aleatória (chance de ser cromada afetada pelo buff do servidor)",
				2500,
				(mb, ch, ms) -> {
					GuildConfig gc = GuildDAO.getGuildById(mb.getGuild().getId());
					Helper.forceSpawnKawaipon(gc, ms, null);
				}));
		put("spawnanime", new Consumable("Invocar Anime",
				"Invoca uma carta aleatória de um anime específico (chance de ser cromada afetada pelo buff do servidor)",
				10000,
				(mb, ch, ms) -> {
					String[] args = ms.getContentRaw().split(" ");
					if (args.length < 3) {
						ch.sendMessage("❌ | Você precisa especificar o anime que deseja que apareça uma carta (colocar `_` no lugar de espaços).").queue();
						return;
					} else if (Arrays.stream(AnimeName.values()).noneMatch(a -> a.name().equals(args[2].toUpperCase()))) {
						ch.sendMessage("❌ | Anime inválido ou ainda não adicionado (colocar `_` no lugar de espaços).").queue();
						return;
					}

					GuildConfig gc = GuildDAO.getGuildById(mb.getGuild().getId());
					Helper.forceSpawnKawaipon(gc, ms, AnimeName.valueOf(args[2].toUpperCase()));
				}));
	}};

	public static Map<String, Consumable> getAvailable() {
		return available;
	}
}
