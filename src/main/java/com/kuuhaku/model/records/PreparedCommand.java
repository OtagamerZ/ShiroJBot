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

package com.kuuhaku.model.records;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumSet;

public record PreparedCommand(
		String name,
		String description,
		Category category,
		Permission[] permissions,
		Executable command
) implements Comparable<PreparedCommand> {

	public String description(I18N code) {
		return code.get(description);
	}

	public boolean canExecute(GuildChannel gc) {
		return gc.getGuild().getSelfMember().hasPermission(gc, permissions);
	}

	public Permission[] getMissingPerms(GuildChannel gc) {
		EnumSet<Permission> required = EnumSet.of(Permission.MESSAGE_WRITE, permissions);
		EnumSet<Permission> missing = EnumSet.noneOf(Permission.class);

		Member self = gc.getGuild().getSelfMember();
		for (Permission permission : required) {
			if (!self.getPermissions(gc).contains(permission))
				missing.add(permission);
		}

		return missing.stream().sorted(Comparator.comparingInt(Permission::getOffset)).toArray(Permission[]::new);
	}

	@Override
	public int compareTo(@NotNull PreparedCommand o) {
		return name.compareTo(o.name);
	}
}