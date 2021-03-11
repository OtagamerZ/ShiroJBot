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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.GuildConfig;
import net.dv8tion.jda.api.entities.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
		name = "ativar",
		aliases = {"ligar", "enable", "activate"},
		usage = "req_commands",
		category = Category.MODERATION
)
public class EnableCommandCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário especificar um ou mais comandos para serem ativados.").queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		Set<Class<?>> disabled = gc.getDisabledCommands().stream()
				.map(s -> {
					try {
						return Class.forName(s);
					} catch (ClassNotFoundException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Set<PreparedCommand> commands = new HashSet<>();
		for (String cmd : args) {
			PreparedCommand e = Main.getCommandManager().getCommand(cmd);
			if (e == null) {
				channel.sendMessage("❌ | O comando `" + cmd + "` não foi encontrado.").queue();
				return;
			} else if (!e.getCategory().isEnabled(guild, author)) {
				channel.sendMessage("❌ | A categoria do comando `" + cmd + "` está desativada.").queue();
				return;
			} else if (!disabled.contains(e.getClass())) {
				channel.sendMessage("❌ | O comando `" + cmd + "` já está ativado.").queue();
				return;
			}

			commands.add(e);
		}

		disabled.removeAll(commands.stream().map(PreparedCommand::getClass).collect(Collectors.toSet()));

		channel.sendMessage("✅ | " + (commands.size() == 1 ? "1 comando ativado" : commands.size() + " comandos ativados") + " com sucesso!").queue();
		gc.saveDisabledCommands(disabled);
		GuildDAO.updateGuildSettings(gc);
	}
}
