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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

import java.util.Objects;

public class MakeLogCommand extends Command {

	public MakeLogCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MakeLogCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MakeLogCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MakeLogCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {

			GuildConfig gc = GuildDAO.getGuildById(guild.getId());
			try {
				Objects.requireNonNull(guild.getTextChannelById(gc.getCanalLog())).delete().queue();
			} catch (Exception ignore) {
			}

			guild.createTextChannel("shiro-log").queue(c -> {
				gc.setCanalLog(c.getId());
				channel.sendMessage("✅ | Canal de log criado com sucesso em " + c.getAsMention()).queue(null, Helper::doNothing);
				GuildDAO.updateGuildSettings(gc);
			});
		} catch (InsufficientPermissionException e) {
			channel.sendMessage("❌ | Não tenho permissões sufficientes para criar um canal.").queue();
		}
	}
}
