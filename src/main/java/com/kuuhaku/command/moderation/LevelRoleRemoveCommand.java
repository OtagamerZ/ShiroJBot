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

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildSettings;
import com.kuuhaku.model.persistent.guild.LevelRole;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.NoResultException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

@Command(
		name = "levelrole",
		path = "remove",
		category = Category.MODERATION
)
@Signature({
		"<role:role:r>",
		"<level:number:r>"
})
@Requires(Permission.MANAGE_ROLES)
public class LevelRoleRemoveCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();

		List<LevelRole> matches;
		if (args.has("role")) {
			Role role = event.roles(0);
			if (role == null) {
				event.channel().sendMessage(locale.get("error/invalid_mention")).queue();
				return;
			}

			matches = settings.getLevelRoles().stream()
					.filter(l -> l.getRole().equals(role))
					.toList();
		} else {
			int lvl = args.getInt("level");
			matches = settings.getLevelRoles().stream()
					.filter(l -> l.getLevel() == lvl)
					.toList();
		}

		if (matches.isEmpty()) {
			event.channel().sendMessage(locale.get("error/role_not_found")).queue();
			return;
		}

		Utils.selectOption(locale, event.channel(), matches,
						lr -> locale.get("str/level", lr.getLevel()) + ": " + lr.getRole().getAsMention(),
						event.user()
				).thenAccept(lr -> {
					if (lr == null) {
						event.channel().sendMessage(locale.get("error/invalid_value")).queue();
						return;
					}

					settings.getLevelRoles().remove(lr);
					settings.save();

					event.channel().sendMessage(locale.get("success/level_role_remove")).queue();
				})
				.exceptionally(t -> {
					if (!(t.getCause() instanceof NoResultException)) {
						Constants.LOGGER.error(t, t);
					}

					return null;
				});
	}
}
