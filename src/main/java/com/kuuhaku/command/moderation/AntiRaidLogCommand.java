/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.moderation;

import com.github.ygimenez.model.Page;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.RaidRegistry;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.FileUpload;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;

@Command(
		name = "antiraid",
		path = "log",
		category = Category.MODERATION
)
@Signature("<id:number>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class AntiRaidLogCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildConfig config = data.config();

		int id = args.getInt("id");
		if (id > 0) {
			RaidRegistry r = DAO.find(RaidRegistry.class, id);
			if (r == null || !r.getGuild().equals(config)) {
				event.channel().sendMessage(locale.get("error/report_not_found")).queue();
				return;
			}

			event.channel().sendFiles(FileUpload.fromData(
					r.toString().getBytes(StandardCharsets.UTF_8),
					"report_" + r.getId() + "-" + r.getStartTimestamp().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt"
			)).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/raid_reports"));

		List<Page> pages = Utils.generatePages(eb, config.getRaids(), 20, 10,
				r -> "`ID: " + r.getId() + "` " + Constants.TIMESTAMP_R.formatted(r.getStartTimestamp().getLong(ChronoField.INSTANT_SECONDS)),
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
