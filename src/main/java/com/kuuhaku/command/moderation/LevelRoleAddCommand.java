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
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

@Command(
		name = "levelrole",
		path = "add",
		category = Category.MODERATION
)
@Signature("<role:role:r> <level:number:r>")
@Requires(Permission.MANAGE_ROLES)
public class LevelRoleAddCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		GuildSettings settings = data.config().getSettings();

		Role role = event.roles(0);
		if (role == null) {
			event.channel().sendMessage(locale.get("error/invalid_mention", 0)).queue();
			return;
		}

		if (event.guild().getSelfMember().canInteract(role)) {
			event.channel().sendMessage(locale.get("error/higher_role")).queue();
			return;
		} else if (settings.getLevelRoles().stream().anyMatch(lr -> lr.getRole().equals(role))) {
			event.channel().sendMessage(locale.get("error/role_already_added")).queue();
			return;
		}

		int lvl = args.getInt("level");
		if (settings.getRolesForLevel(lvl).size() >= 5) {
			event.channel().sendMessage(locale.get("error/too_many_roles")).queue();
			return;
		}

		settings.getLevelRoles().add(new LevelRole(settings, lvl, role));
		settings.save();

		event.channel().sendMessage(locale.get("success/level_role_add")).queue();
	}
}
