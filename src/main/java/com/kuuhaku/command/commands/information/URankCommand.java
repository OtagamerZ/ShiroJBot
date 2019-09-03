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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.ArrayList;
import java.util.List;

public class URankCommand extends Command {

	public URankCommand() {
		super("rank", new String[]{"ranking", "top10"}, "<global>", "Mostra o ranking de usuários do servidor ou global.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		List<com.kuuhaku.model.Member> mbs;
		boolean global = false;
		if (args.length > 0 && args[0].equals("global")) {
			mbs = SQLite.getMemberRank(null, true);
			global = true;
		} else {
			mbs = SQLite.getMemberRank(guild.getId(), false);
		}

		String champ = "1 - " + (global ? "(" + Main.getInfo().getGuildByID(mbs.get(0).getId().replace(mbs.get(0).getMid(), "")).getName() + ") " : "") + Main.getInfo().getUserByID(mbs.get(0).getMid()).getAsTag() + " (Level " + mbs.get(0).getLevel() + ")";
		List<com.kuuhaku.model.Member> sub9 = mbs.subList(1, 10);
		StringBuilder sub9Formatted = new StringBuilder();
		for (int i = 0; i < sub9.size(); i++) {
			sub9Formatted
					.append(i + 2)
					.append(" - ")
					.append((global ? "(" + Main.getInfo().getGuildByID(sub9.get(i).getId().replace(sub9.get(i).getMid(), "")).getName() + ") " : ""))
					.append(Main.getInfo().getUserByID(sub9.get(i).getMid()).getAsTag())
					.append(" (Level ")
					.append(sub9.get(i).getLevel())
					.append(")")
					.append("\n");
		}

		List<MessageEmbed> pages = new ArrayList<>();
		EmbedBuilder eb = new EmbedBuilder();

		for (int x = 1; x < Math.ceil(mbs.size() / 10f); x++) {
			StringBuilder next10 = new StringBuilder();
			for (int i = 10 * x; i < mbs.size(); i++) {
				next10
						.append(i + 2)
						.append(" - ")
						.append((global ? "(" + Main.getInfo().getGuildByID(mbs.get(i).getId().replace(mbs.get(i).getMid(), "")).getName() + ") " : ""))
						.append(Main.getInfo().getUserByID(mbs.get(i).getMid()).getAsTag())
						.append(" (Level ")
						.append(mbs.get(i).getLevel())
						.append(")")
						.append("\n");
			}

			eb.setTitle("Ranking de usuários (" + (global ? "GLOBAL" : "LOCAL") + ")");
			eb.addField(Helper.VOID, next10.toString(), false);
			eb.setThumbnail("http://www.marquishoa.com/wp-content/uploads/2018/01/Ranking-icon.png");
			eb.setColor(Helper.getRandomColor());

			pages.add(eb.build());
			eb.clear();
		}

		eb.setTitle("Ranking de usuários (" + (global ? "GLOBAL" : "LOCAL") + ")");
		eb.addField(champ, sub9Formatted.toString(), false);
		eb.setThumbnail("http://www.marquishoa.com/wp-content/uploads/2018/01/Ranking-icon.png");
		eb.setColor(Helper.getRandomColor());

		pages.add(eb.build());

		channel.sendMessage(pages.get(0)).queue(s -> Helper.paginate(s, pages));
	}
}
