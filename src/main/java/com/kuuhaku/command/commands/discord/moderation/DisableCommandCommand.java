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
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
		name = "desativar",
		aliases = {"desligar", "disable", "deactivate"},
		usage = "req_commands",
		category = Category.MODERATION
)
public class DisableCommandCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário especificar um ou mais comandos para serem desativados.").queue();
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

		Set<Executable> commands = new HashSet<>();
		for (String cmd : args) {
			PreparedCommand e = Main.getCommandManager().getCommand(cmd);

			if (e == null) {
				channel.sendMessage("❌ | O comando `" + cmd + "` não foi encontrado.").queue();
				return;
			} else if (!e.getCategory().isEnabled(guild, author)) {
				channel.sendMessage("❌ | A categoria do comando `" + cmd + "` está desativada.").queue();
				return;
			} else if (Helper.equalsAny(e, EnableCommandCommand.class, this.getClass())) {
				channel.sendMessage("❌ | O comando `" + cmd + "` não pode ser desativado.").queue();
				return;
			} else if (disabled.contains(e.getCommand().getClass())) {
				channel.sendMessage("❌ | O comando `" + cmd + "` já está desativado.").queue();
				return;
			}

			commands.add(e.getCommand());
		}

		disabled.addAll(commands.stream().map(Executable::getClass).collect(Collectors.toSet()));

		channel.sendMessage("✅ | " + (commands.size() == 1 ? "1 comando desativado" : commands.size() + " comandos desativados") + " com sucesso!").queue();
		gc.setDisabledCommands(disabled);
		GuildDAO.updateGuildSettings(gc);
	}
}
