/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.functional.TriFunction;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.List;
import java.util.Locale;

public enum CreditItem {
	XP_BOOST(
			"XP Boost", "Ganha 3500 de XP instantâneamente (valor reduzido se você ganhou XP em menos de 10 segundos)",
			6_500,
			(mb, chn, args) -> {
				com.kuuhaku.model.persistent.Member m = MemberDAO.getMember(mb.getId(), mb.getGuild().getId());
				int lvl = m.getLevel();
				chn.sendMessage(mb.getAsMention() + " utilizou um boost de experiência e ganhou " + m.addXp(3500) + " XP.").queue();
				MemberDAO.saveMember(m);

				boolean lvlUp = m.getLevel() > lvl;
				try {
					GuildConfig gc = GuildDAO.getGuildById(chn.getGuild().getId());
					TextChannel lvlChannel = gc.getLevelChannel();
					if (lvlUp && gc.isLevelNotif()) {
						CollectionHelper.getOr(lvlChannel, chn).sendMessage(mb.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
					}
				} catch (InsufficientPermissionException e) {
					chn.sendMessage(mb.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
				}

				return true;
			}
	),
	SPAWN_CARD(
			"Invocar Carta", "Invoca uma carta aleatória (possui chance de ser cromada)",
			5_000,
			(mb, chn, args) -> {
				GuildConfig gc = GuildDAO.getGuildById(mb.getGuild().getId());
				MiscHelper.forceSpawnKawaipon(gc, chn, mb.getUser(), null, false);

				return true;
			}
	),
	SPAWN_ANIME(
			"Invocar Anime", "Invoca uma carta aleatória de um anime específico (possui chance de ser cromada)",
			8_500,
			(mb, chn, args) -> {
				if (args.length < 2) {
					chn.sendMessage("❌ | Você precisa especificar o anime que deseja que apareça uma carta (colocar `_` no lugar de espaços).").queue();
					return false;
				}

				AddedAnime anime = AddedAnime.find(AddedAnime.class, args[1].toUpperCase(Locale.ROOT));
				List<String> animes = AddedAnime.queryAllNative(String.class, "SELECT a.name FROM AddedAnime a WHERE a.hidden = FALSE");
				if (anime == null) {
					chn.sendMessage("❌ | Anime inválido ou ainda não adicionado, você não quis dizer `" + StringHelper.didYouMean(args[1], animes.toArray(String[]::new)) + "`? (colocar `_` no lugar de espaços)").queue();
					return false;
				}

				GuildConfig gc = GuildDAO.getGuildById(mb.getGuild().getId());
				MiscHelper.forceSpawnKawaipon(gc, chn, mb.getUser(), anime, false);
				return true;
			}
	),
	CARD_STASH_SIZE(
			"Aumentar capacidade do armazém pessoal", "Aumenta a quantidade máxima de cartas armazenadas em seu estoque pessoal em 5",
			15_000,
			(mb, chn, args) -> {
				Account acc = Account.find(Account.class, mb.getId());

				acc.setCardStashCapacity(acc.getCardStashCapacity() + 5);
				acc.save();

				return true;
			}
	),
	SPAWN_CARD_FOIL(
			"Invocar Carta Cromada", "Invoca uma carta cromada aleatória",
			65_000,
			(mb, chn, args) -> {
				GuildConfig gc = GuildDAO.getGuildById(mb.getGuild().getId());
				MiscHelper.forceSpawnKawaipon(gc, chn, mb.getUser(), null, true);
				return true;
			}
	),
	SPAWN_ANIME_FOIL(
			"Invocar Anime Cromado", "Invoca uma carta cromada aleatória de um anime específico",
			150_000,
			(mb, chn, args) -> {
				if (args.length < 2) {
					chn.sendMessage("❌ | Você precisa especificar o anime que deseja que apareça uma carta (colocar `_` no lugar de espaços).").queue();
					return false;
				}

				AddedAnime anime = AddedAnime.find(AddedAnime.class, args[1].toUpperCase(Locale.ROOT));
				List<String> animes = AddedAnime.queryAllNative(String.class, "SELECT a.name FROM AddedAnime a WHERE a.hidden = FALSE");
				if (anime == null) {
					chn.sendMessage("❌ | Anime inválido ou ainda não adicionado, você não quis dizer `" + StringHelper.didYouMean(args[1], animes.toArray(String[]::new)) + "`? (colocar `_` no lugar de espaços)").queue();
					return false;
				}

				GuildConfig gc = GuildDAO.getGuildById(mb.getGuild().getId());
				MiscHelper.forceSpawnKawaipon(gc, chn, mb.getUser(), anime, true);
				return true;
			}
	);

	private final String name;
	private final String desc;
	private final int price;
	private final TriFunction<Member, TextChannel, String[], Boolean> effect;

	CreditItem(String name, String desc, int price, TriFunction<Member, TextChannel, String[], Boolean> effect) {
		this.name = name;
		this.desc = desc;
		this.price = price;
		this.effect = effect;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	public int getPrice() {
		return price;
	}

	public TriFunction<Member, TextChannel, String[], Boolean> getEffect() {
		return effect;
	}
}
