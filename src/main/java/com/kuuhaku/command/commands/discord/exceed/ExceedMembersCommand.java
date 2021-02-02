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

package com.kuuhaku.command.commands.discord.exceed;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "exceedmembros",
		aliases = {"exmembers", "membrosx"},
		category = Category.EXCEED
)
public class ExceedMembersCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!ExceedDAO.hasExceed(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-map-no-exceed")).queue();
			return;
		}

		ExceedEnum ex = ExceedEnum.getByName(ExceedDAO.getExceed(author.getId()));
		List<ExceedMember> members = ExceedDAO.getExceedMembers(ex);
		members.sort(Comparator.comparingLong(ExceedMember::getContribution).reversed());
		Emote e = Main.getShiroShards().getEmoteById(TagIcons.getExceedId(ex));

		assert e != null;
		EmbedBuilder eb = new EmbedBuilder()
				.setTitle(":beginner: | Membros da " + ex.getName())
				.setColor(ex.getPalette())
				.setThumbnail(e.getImageUrl());

		List<Page> pages = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		List<List<ExceedMember>> chunks = Helper.chunkify(members, 10);
		for (List<ExceedMember> ems : chunks) {
			eb.clearFields();
			sb.setLength(0);
			for (ExceedMember em : ems) {
				sb.append("**")
						.append(checkUser(em.getId()))
						.append("** | ")
						.append(Helper.getShortenedValue(em.getContribution(), 1000))
						.append(" PDC")
						.append("\n");
			}
			eb.addField("Total de membros: " + members.size(), sb.toString(), false);
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
		);
	}

	private static String checkUser(String id) {
		try {
			return Main.getInfo().getUserByID(id).getName();
		} catch (Exception e) {
			return ShiroInfo.getLocale(I18n.PT).getString("str_invalid-user");
		}
	}
}
