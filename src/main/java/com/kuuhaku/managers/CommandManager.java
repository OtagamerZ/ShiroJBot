/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.records.PreparedCommand;
import com.kuuhaku.utils.Utils;
import net.dv8tion.jda.api.Permission;
import org.reflections8.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CommandManager {
	private final Reflections refl = new Reflections("com.kuuhaku.command");
	private final Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);

	public CommandManager() {
		Set<String> names = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			String full = params.name();
			if (!params.subname().isBlank()) {
				full += "." + params.subname();
			}

			if (!names.add(full)) {
				Constants.LOGGER.warn("Detected commands with the same name: " + full);
			}

			for (String alias : params.aliases()) {
				if (!names.add(alias)) {
					Constants.LOGGER.warn("Detected commands using the same alias: " + alias);
				}
			}
		}
	}

	public Set<PreparedCommand> getCommands() {
		Set<PreparedCommand> commands = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			extractCommand(commands, cmd, params);
		}

		return commands;
	}

	public Set<PreparedCommand> getCommands(Category category) {
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
		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			String full = params.name();
			if (!params.subname().isBlank()) {
				full += "." + params.subname();
			}

			if (name.equalsIgnoreCase(full) || Utils.equalsAny(name, params.aliases())) {
				Requires req = cmd.getDeclaredAnnotation(Requires.class);
				return new PreparedCommand(
						full,
						params.aliases(),
						"cmd/" + cmd.getSimpleName()
								.replaceFirst("(Command|Reaction)$", "")
								.replaceAll("[a-z](?=[A-Z])", "$0-")
								.toLowerCase(Locale.ROOT),
						params.category(),
						req == null ? new Permission[0] : req.value(),
						buildCommand(cmd)
				);
			}
		}

		return null;
	}

	private void extractCommand(Set<PreparedCommand> commands, Class<?> cmd, Command params) {
		String full = params.name();
		if (!params.subname().isBlank()) {
			full += "." + params.subname();
		}

		Requires req = cmd.getDeclaredAnnotation(Requires.class);
		commands.add(new PreparedCommand(
				full,
				params.aliases(),
				"cmd/" + cmd.getSimpleName()
						.replaceFirst("(Command|Reaction)$", "")
						.replaceAll("[a-z](?=[A-Z])", "$0-")
						.toLowerCase(Locale.ROOT),
				params.category(),
				req == null ? new Permission[0] : req.value(),
				buildCommand(cmd)
		));
	}

	private Executable buildCommand(Class<?> klass) {
		try {
			return (Executable) klass.getConstructor().newInstance();
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
			Constants.LOGGER.error(e, e);
			return null;
		}
	}

	public String[] getCommandSignature(Class<?> klass) {
		Signature sig = klass.getDeclaredAnnotation(Signature.class);
		if (sig == null) return new String[0];

		return sig.value();
	}
}