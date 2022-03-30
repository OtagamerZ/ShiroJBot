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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.time.format.DateTimeFormatter;

@Command(
		name = "info",
		aliases = {"botinfo", "bot"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class BotInfoCommand implements Executable {
	private static final String STR_BOT_INFO_SERVERS = "str_bot-info-servers";

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		eb.setTitle(I18n.getString("str_bot-info-title"))
				.setThumbnail(Main.getSelfUser().getEffectiveAvatarUrl())
				.addField(I18n.getString("str_bot-info-field-1"), Main.getUserByID(ShiroInfo.getNiiChan()).getAsTag(), false);

		StringBuilder sb = new StringBuilder();
		for (String d : ShiroInfo.getDevelopers()) {
			sb.append("`").append(Main.getUserByID(d).getAsTag()).append("`  ");
		}
		eb.addField(I18n.getString("str_bot-info-field-2"), sb.toString(), false)
				.addField(I18n.getString("str_bot-info-field-3"), Main.getSelfUser().getTimeCreated().format(DateTimeFormatter.ofPattern(I18n.getString("date-format"))), false)
				.addField(I18n.getString("str_bot-info-field-4"), I18n.getString(STR_BOT_INFO_SERVERS, Main.getShiro().getGuilds().size()), false)
				.addField(I18n.getString("str_bot-info-field-5"), I18n.getString("str_bot-info-registered-users", StringHelper.separate(MemberDAO.getMemberCount())), false)
				.addField(I18n.getString("str_bot-info-field-6"), ShiroInfo.getVersion(), false)
				.addField("Links:", """
								[%s](https://discordapp.com/invite/9sgkzna)
								[Top.GG](https://top.gg/bot/572413282653306901)
								[%s](https://top.gg/bot/572413282653306901/vote)
								[GitHub](https://github.com/OtagamerZ/ShiroJBot)
								[%s](https://github.com/OtagamerZ/ShiroJBot/blob/master/PRIVACY_POLICY.md)
								[Wiki](https://github.com/OtagamerZ/ShiroJBot/wiki)
								[%s](https://top.gg/bot/572413282653306901/invite)
								[%s](https://forms.gle/KrPHLZcijpzCXDoh9)
								"""
								.formatted(
										I18n.getString("str_support"),
										I18n.getString("str_vote"),
										I18n.getString("str_privacy-policy"),
										I18n.getString("str_invite"),
										I18n.getString("str_unblock-form")
								)
						, false
				)
				.setImage("https://discordbots.org/api/widget/572413282653306901.png?usernamecolor=b463ff&topcolor=000000&middlecolor=1a1d23&datacolor=b463ff&v=" + StringHelper.generateRandomHash(5));

		channel.sendMessageEmbeds(eb.build()).queue();
	}
}
