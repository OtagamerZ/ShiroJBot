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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ClanHierarchy;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ClanCommand extends Command {

	public ClanCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ClanCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ClanCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ClanCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
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
				.addField("Cofre", ":coin: | %s créditos".formatted(c.getVault()), false);

		if (c.getTier() != ClanTier.DYNASTY)
			eb.addField("Metas para promoção", """
					Membros: %s/%s
					Créditos: %s/%s
										
					**Desbloqueia**
					%s
					""".formatted(
					c.getMembers().size(), c.getTier().getCapacity() / 2,
					c.getVault(), c.getTier().getCost(),
					switch (c.getTier()) {
						case PARTY -> """
								Título de facção
								Capacidade de membros (~~10~~ -> 50)
								Mensagem do dia
								""";
						case FACTION -> """
								Título de guilda
								Capacidade de membros (~~50~~ -> 100)
								Emblema
								""";
						case GUILD -> """
								Título de dinastia
								Capacidade de membros (~~100~~ -> 500)
								Banner
								""";
						default -> "";
					}
			), false);

		StringBuilder sb = new StringBuilder();
		List<List<Map.Entry<String, ClanHierarchy>>> mbs = Helper.chunkify(c.getMembers().entrySet(), 10);
		for (List<Map.Entry<String, ClanHierarchy>> chunk : mbs) {
			sb.setLength(0);

			for (Map.Entry<String, ClanHierarchy> mb : chunk) {
				sb.append("`%s` | %s %s\n".formatted(mb.getKey(), mb.getValue().getIcon(), checkUser(mb.getKey())));
			}

			eb.addField("Membros", sb.toString(), false);
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
