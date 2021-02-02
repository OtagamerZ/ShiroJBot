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
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.Helper;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Set;

public class CommandManager {
	/*
	put(DisboardCommand.class, new Arguments(
			"disboard", new String[]{"exmap", "mapa"}, "cmd_disboard", EXCEED, false
	));
	*/

	public Set<PreparedCommand> getCommands() {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
		Set<PreparedCommand> commands = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			extractCommand(commands, cmd, params);
		}

		return commands;
	}

	public Set<PreparedCommand> getCommands(Category category) {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
		Set<PreparedCommand> commands = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params.category() == category) {
				extractCommand(commands, cmd, params);
			}
		}

		return commands;
	}

	public PreparedCommand getCommand(String name) {
		Reflections refl = new Reflections(this.getClass().getPackageName());
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (name.equalsIgnoreCase(params.name()) || Helper.equalsAny(name, params.aliases())) {
				Requires req = cmd.getDeclaredAnnotation(Requires.class);
				return new PreparedCommand(
						params.name(),
						params.aliases(),
						params.usage(),
						"cmd_" + cmd.getSimpleName()
								.replaceAll("Command|Reaction", "")
								.replaceAll("[a-z](?=[A-Z])", "$0-")
								.toLowerCase(),
						params.category(),
						req == null ? null : req.value()
				);
			}
		}

		return null;
	}

	private void extractCommand(Set<PreparedCommand> commands, Class<?> cmd, Command params) {
		Requires req = cmd.getDeclaredAnnotation(Requires.class);
		commands.add(new PreparedCommand(
				params.name(),
				params.aliases(),
				params.usage(),
				"cmd_" + cmd.getSimpleName()
						.replaceAll("Command|Reaction", "")
						.replaceAll("[a-z](?=[A-Z])", "$0-")
						.toLowerCase(),
				params.category(),
				req == null ? null : req.value()
		));
	}
}
