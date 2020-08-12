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

package com.kuuhaku.command.commands.discord.support;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InviteCommand extends Command {

	public InviteCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public InviteCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public InviteCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public InviteCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();

		List<String[]> servers = new ArrayList<>();
		Main.getInfo().getAPI().getGuilds().stream().filter(s -> s.getSelfMember().hasPermission(Permission.CREATE_INSTANT_INVITE)).forEach(g -> servers.add(new String[]{g.getName(), g.getId(), String.valueOf(g.getMembers().stream().filter(m -> !m.getUser().isBot()).count())}));
		List<List<String[]>> svPages = Helper.chunkify(servers, 10);

		List<Page> pages = new ArrayList<>();

		for (int i = 0; i < svPages.size(); i++) {
			eb.clear();

			eb.setTitle("Servidores que eu posso criar um convite:");
			svPages.get(i).forEach(p -> eb.addField("Nome: " + p[0], "ID: " + p[1] + "\nMembros: " + p[2], false));
			eb.setFooter("Página " + (i + 1) + " de " + svPages.size() + ". Total de " + svPages.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		try {
			if (!ShiroInfo.getRequests().contains(args[0])) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_assist-not-requested")).queue();
				return;
			}

			ShiroInfo.getRequests().remove(args[0]);
			Guild guildToInvite = Main.getInfo().getGuildByID(args[0]);
			assert guildToInvite.getDefaultChannel() != null;
			String invite = Helper.createInvite(guildToInvite).setMaxAge((long) 30, TimeUnit.SECONDS).setMaxUses(1).complete().getUrl();
			channel.sendMessage("Aqui está!\n" + invite).queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage("Escolha o servidor que devo criar um convite!\n").embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(m, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
		} catch (NullPointerException ex) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-server")).embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(m, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
		}
	}
}
