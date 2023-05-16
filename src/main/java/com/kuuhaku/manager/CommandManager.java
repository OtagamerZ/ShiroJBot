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

package com.kuuhaku.manager;

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.records.PreparedCommand;
import net.dv8tion.jda.api.Permission;
import org.reflections8.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommandManager {
	private final Reflections refl = new Reflections("com.kuuhaku.command");
	private final Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
	private final Set<String> names = new HashSet<>();

	public CommandManager() {
		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			String full = params.name();
			if (!params.subname().isBlank()) {
				full += "." + params.subname();
			}

			if (!names.add(full)) {
				Constants.LOGGER.fatal("Detected commands with the same name: " + full);
				System.exit(1);
			}
		}
	}

	public Set<PreparedCommand> getCommands() {
		Set<PreparedCommand> commands = new TreeSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			extractCommand(commands, cmd, params);
		}

		return commands;
	}

	public Set<PreparedCommand> getCommands(Category category) {
		Set<PreparedCommand> commands = new TreeSet<>();

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

			if (name.equalsIgnoreCase(full)) {
				Requires req = cmd.getDeclaredAnnotation(Requires.class);
				return new PreparedCommand(
						full,
						"cmd/" + cmd.getSimpleName()
								.replaceFirst("(Command|Reaction)$", "")
								.replaceAll("[a-z](?=[A-Z])", "$0-")
								.toLowerCase(),
						params.category(),
						req == null ? new Permission[0] : req.value(),
						buildCommand(cmd)
				);
			}
		}

		return null;
	}

	public Set<PreparedCommand> getSubCommands(String parent) {
		Set<PreparedCommand> out = new TreeSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params.subname().isBlank() || !params.name().equalsIgnoreCase(parent)) continue;

			Requires req = cmd.getDeclaredAnnotation(Requires.class);
			out.add(new PreparedCommand(
					params.name() + "." + params.subname(),
					"cmd/" + cmd.getSimpleName()
							.replaceFirst("(Command|Reaction)$", "")
							.replaceAll("[a-z](?=[A-Z])", "$0-")
							.toLowerCase(),
					params.category(),
					req == null ? new Permission[0] : req.value(),
					buildCommand(cmd)
			));
		}

		return out;
	}

	private void extractCommand(Set<PreparedCommand> commands, Class<?> cmd, Command params) {
		String full = params.name();
		if (!params.subname().isBlank()) {
			full += "." + params.subname();
		}

		Requires req = cmd.getDeclaredAnnotation(Requires.class);
		commands.add(new PreparedCommand(
				full,
				"cmd/" + cmd.getSimpleName()
						.replaceFirst("(Command|Reaction)$", "")
						.replaceAll("[a-z](?=[A-Z])", "$0-")
						.toLowerCase(),
				params.category(),
				req == null ? new Permission[0] : req.value(),
				buildCommand(cmd)
		));
	}

	private Executable buildCommand(Class<?> klass) {
		try {
			return (Executable) klass.getConstructor().newInstance();
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException |
				 IllegalAccessException e) {
			Constants.LOGGER.error(e, e);
			return null;
		}
	}

	public String[] getCommandSignature(Class<?> klass) {
		Signature sig = klass.getDeclaredAnnotation(Signature.class);
		if (sig == null) return new String[0];

		return sig.value();
	}

	public Set<String> getReservedNames() {
		return names;
	}
}