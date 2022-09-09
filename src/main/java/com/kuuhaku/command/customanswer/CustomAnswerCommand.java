/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.customanswer;

import com.github.ygimenez.model.Page;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.CustomAnswer;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Command(
		name = "custom",
		category = Category.MODERATION
)
@Signature("<id:number>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class CustomAnswerCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();

		int id = args.getInt("id");
		if (id > 0) {
			CustomAnswer ca = settings.getCustomAnswers().stream()
					.filter(c -> c.getId().getId() == id)
					.findFirst().orElse(null);

			if (ca == null) {
				event.channel().sendMessage(locale.get("error/id_not_found")).queue();
				return;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/custom_answer"));

			XStringBuilder sb = new XStringBuilder();
			sb.appendNewLine(locale.get("str/ca_trigger", StringUtils.abbreviate(ca.getTrigger().replace("`", "'"), 20)));
			sb.appendNewLine(locale.get("str/ca_answer", StringUtils.abbreviate(ca.getAnswer().replace("`", "'"), 20)));

			if (ca.getChance() < 100) {
				sb.appendNewLine(locale.get("str/ca_chance", ca.getChance()));
			}

			if (!ca.getChannels().isEmpty()) {
				List<String> ments = new ArrayList<>();
				for (Object o : ca.getChannels()) {
					if (ments.size() == 5) {
						ments.add(locale.get("str/and_more", ca.getChannels().size() - ments.size()));
						break;
					}

					TextChannel chn = event.guild().getTextChannelById(String.valueOf(o));
					if (chn != null) {
						ments.add(chn.getAsMention());
					}
				}

				sb.appendNewLine(locale.get("str/ca_channels", String.join(" ", ments)));
			}

			if (!ca.getUsers().isEmpty()) {
				List<String> ments = new ArrayList<>();
				for (Object o : ca.getUsers()) {
					if (ments.size() == 5) {
						ments.add(locale.get("str/and_more", ca.getUsers().size() - ments.size()));
						break;
					}

					User usr = bot.getUserById(String.valueOf(o));
					if (usr != null) {
						ments.add(usr.getAsMention());
					}
				}

				sb.appendNewLine(locale.get("str/ca_users", String.join(" ", ments)));
			}

			eb.setDescription(sb.toString());
			event.channel().sendMessageEmbeds(eb.build()).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/custom_answers"));

		List<Page> pages = Utils.generatePages(eb, settings.getCustomAnswers(), 20, 10,
				ca -> locale.get("str/ca_trigger", StringUtils.abbreviate(ca.getTrigger().replace("`", "'"), 20)) +
						"\n" +
						locale.get("str/ca_answer", StringUtils.abbreviate(ca.getAnswer().replace("`", "'"), 20)),
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
