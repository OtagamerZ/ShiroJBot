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
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.persistent.guild.LevelRole;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Command(
		name = "levelrole",
		category = Category.MODERATION
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MANAGE_ROLES
})
public class LevelRoleCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();
		if (settings.getLevelRoles().isEmpty()) {
			event.channel().sendMessage(locale.get("error/no_level_roles")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/level_roles"));

		List<String> roles = settings.getLevelRoles().stream()
				.collect(Collectors.groupingBy(LevelRole::getLevel))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(e -> locale.get("str/level", e.getKey()) + ": " + Utils.properlyJoin(locale.get("str/and"))
						.apply(e.getValue().stream()
								.map(LevelRole::getRole)
								.map(r -> r == null ? locale.get("str/unknown") : r.getAsMention())
								.toList()
						)
				).toList();

		List<Page> pages = Utils.generatePages(eb, roles, 20, 10, Function.identity(),
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
