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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.sqlite.BackupDAO;
import com.kuuhaku.controller.sqlite.CustomAnswerDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.CustomAnswer;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "fale",
		aliases = {"custom"},
		usage = "req_trigger-response",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class CustomAnswerCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!Helper.hasPermission(member, PrivilegeLevel.MOD) && GuildDAO.getGuildById(guild.getId()).isNotAnyTell()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_custom-answer-community-disabled")).queue();
			return;
		} else if (args.length == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_custom-answer-not-enough-args")).queue();
			return;
		} else if (args[0].equals("lista")) {
			List<Page> pages = new ArrayList<>();

			List<CustomAnswer> ca = BackupDAO.getCADump();
			EmbedBuilder eb = new ColorlessEmbedBuilder();
			ca.removeIf(a -> !a.getGuildID().equals(guild.getId()) || a.isMarkForDelete());

			for (int x = 0; x < Math.ceil(ca.size() / 10f); x++) {
				eb.clear();
				eb.setTitle(":pencil: Respostas deste servidor:");
				for (int i = -10 + (10 * (x + 1)); i < ca.size() && i < (10 * (x + 1)); i++) {
					eb.addField(ca.get(i).getId() + " - " + ca.get(i).getGatilho(), ca.get(i).getAnswer().length() > 100 ? ca.get(i).getAnswer().substring(0, 100) + "..." : ca.get(i).getAnswer(), false);
				}
				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			if (pages.size() == 0) {
				channel.sendMessage("Não há nenhuma resposta cadastrada neste servidor.").queue();
				return;
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
			return;
		} else if (StringUtils.isNumeric(args[0]) && !args[0].contains(";")) {
			List<CustomAnswer> ca = BackupDAO.getCADump();
			ca.removeIf(a -> !String.valueOf(a.getId()).equals(args[0]) || !a.getGuildID().equals(guild.getId()));
			if (ca.size() == 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_custom-answer-not-found")).queue();
				return;
			}
			CustomAnswer c = ca.get(0);

			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle(":speech_balloon: Resposta Nº " + c.getId());
			eb.addField(":arrow_right: " + c.getGatilho(), ":arrow_left: " + c.getAnswer(), false);

			channel.sendMessage(eb.build()).queue();
			return;
		}

		String[] txt = String.join(" ", args).split(";");

		if (txt.length > 1) {
			if (txt[0].length() <= 200) {
				if (txt[1].length() <= 200) {
					CustomAnswerDAO.addCAtoDB(guild, txt[0].trim(), txt[1].trim());
					channel.sendMessage("Agora quando alguém disser `" + txt[0] + "` irei responder `" + txt[1] + "`.").queue();
				} else {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_custom-answer-reply-too-long")).queue();
				}
			} else {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_custom-answer-trigger-too-long")).queue();
			}
		} else {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_custom-answer-invalid-arguments")).queue();
		}
	}
}
