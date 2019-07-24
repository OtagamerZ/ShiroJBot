/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class URankCommand extends Command {

	public URankCommand() {
		super("rank", new String[]{"ranking", "top10"}, "<global>", "Mostra o ranking de usuários do servidor ou global.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		List<com.kuuhaku.model.Member> mbs;

		if (args.length == 0) {
			mbs = SQLite.getMemberRank(guild.getId(), false);
			if (mbs.size() > 7) mbs.subList(7, mbs.size()).clear();
		} else if (args[0].equalsIgnoreCase("global")) {
			mbs = SQLite.getMemberRank(guild.getId(), true);
			if (mbs.size() > 7) mbs.subList(7, mbs.size()).clear();
		} else {
			channel.sendMessage(":x: | O único parâmetro permitido após o comando é `global`.").queue();
			return;
		}

		channel.sendMessage("<a:Loading:598500653215645697> Buscando dados...").queue(m -> {
			try {
				EmbedBuilder eb = new EmbedBuilder();
				StringBuilder sb = new StringBuilder();

				eb.setTitle(":bar_chart: TOP 10 Usuários (" + (args.length > 0 && args[0].equalsIgnoreCase("global") ? "GLOBAL" : "SERVER") + ")");
				eb.setThumbnail(args.length > 0 && args[0].equalsIgnoreCase("global") ? "https://www.pngkey.com/png/full/21-217733_free-png-trophy-png-images-transparent-winner-trophy.png" : guild.getIconUrl());
				try {
					eb.setColor(Helper.colorThief(Main.getInfo().getUserByID(mbs.get(0).getMid()).getAvatarUrl()));
				} catch (IOException e) {
					eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
				}

				for (com.kuuhaku.model.Member mb : mbs) {
					try {
						Guild g = Main.getInfo().getGuildByID(mb.getId().substring(18));
						g.getName();
						g.getMemberById(mb.getMid()).getEffectiveName();
						//noinspection ResultOfMethodCallIgnored
						mb.getLevel();
						//noinspection ResultOfMethodCallIgnored
						mb.getXp();
					} catch (Exception e) {
						mbs.remove(mb);
					}
				}

				for (int i = 1; i < mbs.size() && i < 10; i++) {
					Guild g = Main.getInfo().getGuildByID(mbs.get(i).getId().substring(18));
					sb.append(i + 1).append(" - ").append(args.length == 0 ? " " : ("(" + g.getName() + ") ")).append(g.getMemberById(mbs.get(i).getMid()).getEffectiveName()).append(" - Lvl ").append(mbs.get(i).getLevel()).append(" (").append(mbs.get(i).getXp()).append(" xp)\n");
				}
				eb.addField("1 - " + (args.length == 0 ? " " : ("(" + Main.getInfo().getGuildByID(mbs.get(0).getId().substring(18)).getName() + ") ")) + Main.getInfo().getGuildByID(mbs.get(0).getId().substring(18)).getMemberById(mbs.get(0).getMid()).getEffectiveName() + " - Lvl" + mbs.get(0).getLevel() + " (" + mbs.get(0).getXp() + " xp)", sb.toString(), false);

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
			} catch (NullPointerException e) {
				m.editMessage(":x: | Erro, o ranking global está com problemas no momento, já estamos trabalhando em uma solução.").queue();
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
