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
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import org.jetbrains.annotations.NonNls;

public class ReactionArgument extends Argument {
	private final boolean answerable;
	private final String type;

	public ReactionArgument(@NonNls String name, @NonNls String[] aliases, String description, boolean answerable, @NonNls String type) {
		super(name, aliases, description, Category.FUN, false);
		this.answerable = answerable;
		this.type = type;
	}

	public Object[] getArguments() {
		return new Object[]{
				getName(),
				getAliases(),
				ShiroInfo.getLocale(I18n.PT).getString(getDescription()),
				answerable,
				type
		};
	}

	public boolean isAnswerable() {
		return answerable;
	}

	public String getType() {
		return type;
	}
}
