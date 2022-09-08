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

package com.kuuhaku.command.moderation;

import com.github.ygimenez.model.Page;
import com.kuuhaku.Main;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import java.util.List;
import java.util.Locale;

@Command(
		name = "alias",
		category = Category.MODERATION
)
@Signature(allowEmpty = true, value = {
		"<command:word:r> <alias:word:r>"
})
public class AliasCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		JSONObject aliases = data.config().getSettings().getAliases();
		if (args.isEmpty()) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/alias"));

			List<Page> pages = Utils.generatePages(eb, aliases.entrySet(), 20, 10,
					e -> "`" + e.getKey() + "` -> `" + e.getValue() + "`",
					(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
			);

			Utils.paginate(pages, 1, true, event.channel(), event.user());
			return;
		}

		String cmd = args.getString("command").toLowerCase(Locale.ROOT);
		String alias = args.getString("alias").toLowerCase(Locale.ROOT);
		if (!Main.getCommandManager().getReservedNames().contains(cmd)) {
			event.channel().sendMessage(locale.get("error/command_not_found")).queue();
			return;
		} else if (Main.getCommandManager().getReservedNames().contains(alias)) {
			event.channel().sendMessage(locale.get("error/reserved_name")).queue();
			return;
		} else if (aliases.containsKey(alias)) {
			event.channel().sendMessage(locale.get("error/existing_alias")).queue();
			return;
		}

		aliases.values().remove(cmd);
		aliases.put(alias, cmd);
		data.config().save();

		event.channel().sendMessage(locale.get("success/alias_add")).queue();
	}
}
