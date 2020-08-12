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
import com.kuuhaku.command.TwitchCommand;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class TwitchCommandManager {
	private final HashMap<Class<? extends TwitchCommand>, Argument> commands = new HashMap<>() {
		{

		}
	};

	public HashMap<Class<? extends TwitchCommand>, Argument> getCommands() {
		return commands;
	}

	public TwitchCommand getCommand(String name) {
		Map.Entry<Class<? extends TwitchCommand>, Argument> cmd = commands.entrySet().stream().filter(e -> e.getValue().getName().equalsIgnoreCase(name) || ArrayUtils.contains(e.getValue().getAliases(), name.toLowerCase())).findFirst().orElse(null);

		if (cmd == null) return null;

		try {
			if (cmd.getValue() instanceof ReactionArgument)
				//noinspection JavaReflectionInvocation
				return cmd.getKey()
						.getConstructor(String.class, String[].class, String.class, boolean.class, String.class)
						.newInstance(cmd.getValue().getArguments());
			else
				//noinspection JavaReflectionInvocation
				return cmd.getKey()
						.getConstructor(String.class, String[].class, String.class, String.class, Category.class, boolean.class)
						.newInstance(cmd.getValue().getArguments());
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}
}
