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
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "avatar",
		aliases = {"pfp"},
		usage = "req_mention-guild",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class AvatarCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		eb.setColor(Helper.getRandomColor());

		if (args.length > 0) {
			if (Helper.equalsAny(args[0], "guild", "server", "servidor")) {
				if (guild.getIconUrl() == null) {
					channel.sendMessage(I18n.getString("err_no-icon")).queue();
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
