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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.model.annotations.*;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.records.SlashParam;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.reflections8.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommandManager {
	private final Reflections refl = new Reflections("com.kuuhaku.command.commands");
	private final Set<Class<?>> cmds = refl.getTypesAnnotatedWith(Command.class);
	private final Map<String, Set<Class<?>>> slashes = new HashMap<>();

	public CommandManager() {
		Set<String> names = new HashSet<>();

		for (Class<?> cmd : cmds) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			if (!names.add(params.name())) {
				Helper.logger(this.getClass()).warn("Detectado comando com nome existente: " + params.name());
			}

			for (String alias : params.aliases()) {
				if (!names.add(alias)) {
					Helper.logger(this.getClass()).warn("Detectado comando com alias existente: " + alias);
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
			if (name.equalsIgnoreCase(params.name()) || Helper.equalsAny(name, params.aliases())) {
				Requires req = cmd.getDeclaredAnnotation(Requires.class);
				return new PreparedCommand(
						params.name(),
						params.aliases(),
						params.usage(),
						"cmd_" + cmd.getSimpleName()
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

	public PreparedCommand getSlash(String name, String sub) {
		for (Class<?> cmd : slashes.get(name)) {
			Command params = cmd.getDeclaredAnnotation(Command.class);
			SlashCommand slash = cmd.getDeclaredAnnotation(SlashCommand.class);
			if (slash.name().equals(sub)) {
				Requires req = cmd.getDeclaredAnnotation(Requires.class);
				return new PreparedCommand(
						params.name(),
						params.aliases(),
						params.usage(),
						"cmd_" + cmd.getSimpleName()
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
		Requires req = cmd.getDeclaredAnnotation(Requires.class);
		commands.add(new PreparedCommand(
				params.name(),
				params.aliases(),
				params.usage(),
				"cmd_" + cmd.getSimpleName()
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
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	public void registerCommands() {
		Set<Class<?>> cmds = refl.getTypesAnnotatedWith(SlashCommand.class);
		for (Class<?> cmd : cmds) {
			if (cmd.isAnnotationPresent(SlashGroup.class)) {
				SlashGroup group = cmd.getDeclaredAnnotation(SlashGroup.class);
				slashes.computeIfAbsent(group.value(), k -> new HashSet<>()).add(cmd);
			} else {
				slashes.computeIfAbsent(null, k -> new HashSet<>()).add(cmd);
			}
		}

		List<CommandData> cds = new ArrayList<>();
		for (Map.Entry<String, Set<Class<?>>> entries : slashes.entrySet()) {
			String group = entries.getKey();
			if (group != null) {
				List<SubcommandData> sds = new ArrayList<>();
				for (Class<?> klass : entries.getValue()) {
					SlashCommand cmd = klass.getDeclaredAnnotation(SlashCommand.class);
					SubcommandData sd = new SubcommandData(
							cmd.name(),
							I18n.getString(
									"cmd_" + klass.getSimpleName()
											.replaceFirst("(Command|Reaction)$", "")
											.replaceAll("[a-z](?=[A-Z])", "$0-")
											.toLowerCase(Locale.ROOT)
							)
					);
					List<SlashParam> params = new ArrayList<>();
					for (String arg : cmd.args()) {
						params.add(JSONUtils.fromJSON(arg, SlashParam.class));
					}

					for (SlashParam param : params) {
						sd.addOption(param.type(), param.name(), param.description(), param.required());
					}

					sds.add(sd);
				}

				cds.add(new CommandData(group, "Categoria " + group.toUpperCase())
						.addSubcommands(sds));
			} else {
				for (Class<?> klass : entries.getValue()) {
					SlashCommand cmd = klass.getDeclaredAnnotation(SlashCommand.class);
					CommandData cd = new CommandData(
							cmd.name(),
							I18n.getString(
									"cmd_" + klass.getSimpleName()
											.replaceFirst("(Command|Reaction)$", "")
											.replaceAll("[a-z](?=[A-Z])", "$0-")
											.toLowerCase(Locale.ROOT)
							)
					);
					List<SlashParam> params = new ArrayList<>();
					for (String arg : cmd.args()) {
						params.add(JSONUtils.fromJSON(arg, SlashParam.class));
					}

					for (SlashParam param : params) {
						cd.addOption(param.type(), param.name(), param.description(), param.required());
					}

					cds.add(cd);
				}
			}
		}

		Main.getDefaultShard().updateCommands().queue();
		//Main.getDefaultShard().updateCommands().addCommands(cds).complete();
		Helper.logger(this.getClass()).info(slashes.values().stream().mapToLong(Set::size).sum() + " comandos Slash registrados.");
	}

	public String[] getCommandSignature(Class<?> klass) {
		Signature sig = klass.getDeclaredAnnotation(Signature.class);
		if (sig == null) return new String[0];

		return sig.value();
	}
}