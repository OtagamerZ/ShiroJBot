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

package com.kuuhaku.command.commands.discord.support;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.stream.Collectors;

public class IDCommand extends Command {

	public IDCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public IDCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public IDCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public IDCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length > 0) {
			try {
				String arg = String.join(" ", args);
				String ex = Helper.containsAll(arg, "[", "]") ? arg.substring(arg.indexOf("["), arg.indexOf("]") + 1) : "";
				String name = arg.replace(ex, "").trim();
				List<User> us = Main.getInfo().getAPI().getUsersByName(name, true);
				EmbedBuilder eb = new EmbedBuilder();

				eb.setTitle("IDs dos usuários encontrados");
				for (User u : us) {
					eb.addField(
							u.getAsTag() + ": " + u.getId(),
							"Guilds: " + u.getMutualGuilds().stream().map(g -> " `" + g.getName() + "` ").collect(Collectors.joining()),
							false
					);
				}
				eb.setColor(Helper.getRandomColor());

				channel.sendMessage(eb.build()).queue();
			} catch (InsufficientPermissionException ex) {
                channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_send-embed")).queue();
            } catch (Exception e) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-not-found")).queue();
            }
		} else {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-name")).queue();
        }
	}

}
