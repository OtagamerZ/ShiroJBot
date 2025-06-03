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
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.records.PreparedCommand;
import net.dv8tion.jda.api.Permission;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommandManager {
	private final Reflections refl = new Reflections("com.kuuhaku.command");
	private final Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
	private final Set<String> names = new HashSet<>();
	private final Map<String, PreparedCommand> surrogate = new HashMap<>();
	private final Map<String, PreparedCommand> mapped = new HashMap<>();

	public CommandManager() {
		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params == null) continue;

			String full = params.name();
			if (params.path().length > 0) {
				full += "." + String.join(".", params.path());
			}

			full = full.toLowerCase();
			if (!names.add(full)) {
				Constants.LOGGER.fatal("Detected commands with the same name: {}", full);
				System.exit(1);
			}
		}

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params == null) continue;

			String parent = params.name();

			if (!names.contains(parent) && !surrogate.containsKey(parent)) {
				surrogate.put(parent, new PreparedCommand(parent, params.category(), null, null, params.beta()));
				Constants.LOGGER.info("Mapped surrogate command parent: {}", parent);
			}
		}
	}

	public Set<PreparedCommand> getCommands() {
		Set<PreparedCommand> commands = new TreeSet<>(Comparator.comparing(PreparedCommand::name));

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params != null) {
				extractCommand(commands, cmd, params);
			}
		}

		return commands;
	}

	public Set<PreparedCommand> getCommands(Category category) {
		Set<PreparedCommand> commands = new TreeSet<>(Comparator.comparing(PreparedCommand::name));

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params.category() == category) {
				extractCommand(commands, cmd, params);
			}
		}

		return commands;
	}

	public PreparedCommand getCommand(String name) {
		name = name.toLowerCase();
		if (!names.contains(name)) {
			return surrogate.get(name);
		}

		if (mapped.containsKey(name)) return mapped.get(name);
		else {
			for (Class<?> cmd : cmds) {
				Command params = cmd.getDeclaredAnnotation(Command.class);
				if (params == null) continue;

				String full = params.name();
				if (params.path().length > 0) {
					full += "." + String.join(".", params.path());
				}

				if (name.equals(full)) {
					Requires req = cmd.getDeclaredAnnotation(Requires.class);
					PreparedCommand pc = new PreparedCommand(
							full,
							params.category(),
							req == null ? new Permission[0] : req.value(),
							buildCommand(cmd),
							params.beta()
					);

					mapped.put(full, pc);
					return pc;
				}
			}
		}

		return null;
	}

	public Set<PreparedCommand> getSubCommands(String parent) {
		Set<PreparedCommand> out = new TreeSet<>(Comparator.comparing(PreparedCommand::name));

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (params == null || params.path().length == 0 || !params.name().equals(parent)) continue;

			extractCommand(out, cmd, params);
		}

		return out;
	}

	private void extractCommand(Set<PreparedCommand> commands, Class<?> cmd, Command params) {
		String full = params.name();
		if (params.path().length > 0) {
			full += "." + String.join(".", params.path());
		}

		if (mapped.containsKey(full)) {
			commands.add(mapped.get(full));
		} else {
			Requires req = cmd.getDeclaredAnnotation(Requires.class);
			commands.add(new PreparedCommand(
					full,
					params.category(),
					req == null ? new Permission[0] : req.value(),
					buildCommand(cmd),
					params.beta()
			));
		}
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

	public String[] getCommandSyntax(Class<?> klass) {
		Syntax sig = klass.getDeclaredAnnotation(Syntax.class);
		if (sig == null) return new String[0];

		return sig.value();
	}

	public Set<String> getReservedNames() {
		return names;
	}

	public Set<Permission> getAllPermissions() {
		Set<Permission> perms = EnumSet.noneOf(Permission.class);
		for (Class<?> cmd : cmds) {
			Requires req = cmd.getDeclaredAnnotation(Requires.class);
			if (req != null) {
				perms.addAll(Set.of(req.value()));
			}
		}

		return perms;
	}
}
