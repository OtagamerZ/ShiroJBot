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

package com.kuuhaku.command.commands;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Comparator;
import java.util.EnumSet;

public class PreparedCommand implements Executable {
	private final String name;
	private final String[] aliases;
	private final String usage;
	private final String description;
	private final Category category;
	private final Permission[] permissions;
	private final Executable command;

	public PreparedCommand(String name, String[] aliases, String usage, String description, Category category, Permission[] permissions, Executable command) {
		this.name = name;
		this.aliases = aliases;
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.permissions = permissions;
		this.command = command;
	}

	public String getName() {
		return name;
	}

	public String[] getAliases() {
		return aliases;
	}

	public String getUsage() {
		return usage.isBlank() ? "" : ShiroInfo.getLocale(I18n.PT).getString(usage);
	}

	public String getDescription() {
		return description.isBlank() ? "" : ShiroInfo.getLocale(I18n.PT).getString(description);
	}

	public Category getCategory() {
		return category;
	}

	public Permission[] getPermissions() {
		return permissions;
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
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		this.command.execute(author, member, command, argsAsText, args, message, channel, guild, prefix);
	}
}
