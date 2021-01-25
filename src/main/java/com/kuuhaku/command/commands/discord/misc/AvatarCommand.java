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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;


public class AvatarCommand extends Command {

	public AvatarCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AvatarCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AvatarCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AvatarCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		eb.setColor(Helper.getRandomColor());

		if (args.length > 0) {
			if (Helper.equalsAny(args[0], "guild", "server", "servidor")) {
				if (guild.getIconUrl() == null) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-icon")).queue();
					return;
				}
				eb.setTitle("Ícone do servidor");
				eb.setImage(guild.getIconUrl() + "?size=4096");
				eb.setDescription("[Clique aqui se não conseguir ver](" + guild.getIconUrl() + "?size=4096)");
			} else if (message.getMentionedUsers().size() > 0) {
				if (author.getId().equals(message.getMentionedUsers().get(0).getId())) {
					eb.setTitle("Seu avatar");
					eb.setImage(author.getEffectiveAvatarUrl() + "?size=4096");
					eb.setDescription("[Clique aqui se não conseguir ver](" + author.getEffectiveAvatarUrl() + "?size=4096)");
				} else {
					eb.setTitle("Avatar de: " + message.getMentionedUsers().get(0).getAsTag());
					eb.setImage(message.getMentionedUsers().get(0).getEffectiveAvatarUrl() + "?size=4096");
					eb.setDescription("[Clique aqui se não conseguir ver](" + message.getMentionedUsers().get(0).getEffectiveAvatarUrl() + "?size=4096)");
				}
			} else {
				eb.setTitle("Seu avatar");
				eb.setImage(author.getEffectiveAvatarUrl() + "?size=4096");
				eb.setDescription("[Clique aqui se não conseguir ver](" + author.getEffectiveAvatarUrl() + "?size=4096)");
			}
		} else {
			eb.setTitle("Seu avatar");
			eb.setImage(author.getEffectiveAvatarUrl() + "?size=4096");
			eb.setDescription("[Clique aqui se não conseguir ver](" + author.getEffectiveAvatarUrl() + "?size=4096)");
		}
		channel.sendMessage(eb.build()).queue();
	}
}
