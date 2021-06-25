/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.managers;

import com.kuuhaku.model.enums.I18n;
import org.jetbrains.annotations.NonNls;

public class TwitchArgument {

	private final String name;
	private final String[] aliases;
	private final String usage;
	private final String description;
	private final boolean requiresBinding;

	public TwitchArgument(@NonNls String name, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = "";
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	public TwitchArgument(@NonNls String name, @NonNls String[] aliases, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = aliases;
		this.usage = "";
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	public TwitchArgument(@NonNls String name, String usage, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = usage;
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	public TwitchArgument(@NonNls String name, @NonNls String[] aliases, String usage, String description, boolean requiresBinding) {
		this.name = name;
		this.aliases = aliases;
		this.usage = usage;
		this.description = description;
		this.requiresBinding = requiresBinding;
	}

	public Object[] getArguments() {
		return new Object[]{
				name,
				aliases,
				usage.isBlank() ? "" : I18n.getString(usage),
				description.isBlank() ? "" : I18n.getString(description),
				requiresBinding
		};
	}

	public String getName() {
		return name;
	}

	public String[] getAliases() {
		return aliases;
	}

	public String getUsage() {
		if (usage.isBlank()) return "";
		return I18n.getString(usage);
	}

	public String getDescription() {
		if (description.isBlank()) return "";
		return I18n.getString(description);
	}

	public boolean requiresBinding() {
		return requiresBinding;
	}
}
