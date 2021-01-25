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

package com.kuuhaku.command;

import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public abstract class Command {

	private final String name;
	private final String[] aliases;
	private final String usage;
	private final String description;
	private final Category category;
	private final boolean requiresMM;

	protected Command(@NonNls String name, String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = null;
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	protected Command(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = aliases;
		this.usage = null;
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	protected Command(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	protected Command(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = aliases;
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	public String getName() {
		return name;
	}

	public String[] getAliases() {
		return aliases;
	}
	
	public String getUsage() {
		return usage;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public boolean requiresMM() {
		return requiresMM;
	}

	public abstract void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix);
}
