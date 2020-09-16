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

package com.kuuhaku.managers;

import com.kuuhaku.command.Category;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import org.jetbrains.annotations.NonNls;

public class Argument {
	private final String name;
	private final String[] aliases;
	private final String usage;
	private final String description;
	private final Category category;
	private final boolean requiresMM;

	public Argument(@NonNls String name, @NonNls String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = "";
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	public Argument(@NonNls String name, @NonNls String[] aliases, @NonNls String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = aliases;
		this.usage = "";
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	public Argument(@NonNls String name, @NonNls String usage, @NonNls String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = new String[]{};
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	public Argument(@NonNls String name, @NonNls String[] aliases, @NonNls String usage, @NonNls String description, Category category, boolean requiresMM) {
		this.name = name;
		this.aliases = aliases;
		this.usage = usage;
		this.description = description;
		this.category = category;
		this.requiresMM = requiresMM;
	}

	public Object[] getArguments() {
		return new Object[]{
				name,
				aliases,
				usage.isBlank() ? "" : ShiroInfo.getLocale(I18n.PT).getString(usage),
				description.isBlank() ? "" : ShiroInfo.getLocale(I18n.PT).getString(description),
				category,
				requiresMM
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
		return ShiroInfo.getLocale(I18n.PT).getString(usage);
	}

	public String getDescription() {
		if (description.isBlank()) return "";
		return ShiroInfo.getLocale(I18n.PT).getString(description);
	}

	public Category getCategory() {
		return category;
	}

	public boolean requiresMM() {
		return requiresMM;
	}
}
