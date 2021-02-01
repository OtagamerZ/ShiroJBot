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

package com.kuuhaku.command.commands.discord.clan;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ClanHierarchy;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "cla",
		aliases = {"clan", "party", "faction", "guild", "dynasty"},
		usage = "req_clan",
		category = Category.CLAN
)
public class ClanCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		}

		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(c.getTier().getName() + " " + c.getName())
				.setThumbnail("attachment://icon.jpg")
				.setImage("attachment://banner.jpg")
				.setDescription(c.getMotd())
				.addField("Cofre", ":coin: | %s%s créditos".formatted(Helper.separate(c.getVault()), c.getTier() != ClanTier.DYNASTY ? "/" + Helper.separate(c.getTier().getVaultSize()) : ""), false);

		if (c.getTier() != ClanTier.DYNASTY)
			eb.addField("Metas para promoção", """
					Membros: %s/%s
					Créditos: %s/%s
										
					**Desbloqueia**
					%s
					""".formatted(
					c.getMembers().size(), c.getTier().getCapacity() / 2,
					Helper.separate(c.getVault()), Helper.separate(c.getTier().getCost()),
					switch (c.getTier()) {
						case PARTY -> """
								Título de facção
								Capacidade de membros (~~10~~ -> 50)
								Capacidade do cofre (~~100.000~~ -> 500.000)
								Mensagem do dia
								""";
						case FACTION -> """
								Título de guilda
								Capacidade de membros (~~50~~ -> 100)
								Capacidade do cofre (~~500.000~~ -> 2.000.000)
								Emblema
								""";
						case GUILD -> """
								Título de dinastia
								Capacidade de membros (~~100~~ -> 500)
								Capacidade do cofre (~~2.000.000~~ -> ilimitado)
								Banner
								""";
						default -> "";
					}
			), false);

		StringBuilder sb = new StringBuilder();
		List<Map.Entry<String, ClanHierarchy>> mbs = new ArrayList<>(c.getMembers().entrySet());
		mbs.sort(Map.Entry.
				<String, ClanHierarchy>comparingByValue(Comparator.comparingInt(ClanHierarchy::ordinal))
				.thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
		);
		List<MessageEmbed.Field> fixed = new ArrayList<>(eb.getFields());
		List<List<Map.Entry<String, ClanHierarchy>>> chunks = Helper.chunkify(mbs, 10);
		for (List<Map.Entry<String, ClanHierarchy>> chunk : chunks) {
			sb.setLength(0);

			for (Map.Entry<String, ClanHierarchy> mb : chunk) {
				sb.append("`%s` | %s %s\n".formatted(mb.getKey(), mb.getValue().getIcon(), checkUser(mb.getKey())));
			}

			eb.clearFields().getFields().addAll(fixed);
			eb.addField("Membros (%s/%s)".formatted(c.getMembers().size(), c.getTier().getCapacity()), sb.toString(), false);
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		MessageAction ma = channel.sendMessage((MessageEmbed) pages.get(0).getContent());
		if (c.getIcon() != null) ma = ma.addFile(Helper.getBytes(c.getIcon()), "icon.jpg");
		if (c.getBanner() != null) ma = ma.addFile(Helper.getBytes(c.getBanner()), "banner.jpg");
		ma.queue(s ->
				Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}

	private static String checkUser(String id) {
		try {
			return Main.getInfo().getUserByID(id).getName();
		} catch (Exception e) {
			return "`Desconhecido`";
		}
	}
}
