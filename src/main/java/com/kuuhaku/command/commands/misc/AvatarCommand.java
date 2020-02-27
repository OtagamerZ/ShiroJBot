/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;


public class AvatarCommand extends Command {

	public AvatarCommand() {
		super("avatar", "<@usuário/guild>", "Dá-lhe o seu avatar ou então o avatar da pessoa mencionada. Para pegar o ícone do servidor digite apenas guild no lugar da menção.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		if (message.getMentionedUsers().size() > 1) {
			channel.sendMessage(":x: | Você só pode mencionar 1 utilizador de cada vez.").queue();
			return;
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Helper.getRandomColor());


		if (message.getMentionedUsers().size() == 0) {
			if (args.length > 0) {
				if (args[0].trim().equalsIgnoreCase("guild")) {
					if (guild.getIconUrl() == null) {
						channel.sendMessage(":x: | O servidor não possui ícone.").queue();
						return;
					}
					eb.setTitle("Ícone do servidor");
					eb.setImage(guild.getIconUrl() + "?size=4096");
					try {
						eb.setColor(Helper.colorThief(guild.getIconUrl()));
					} catch (IOException ignore) {
					}
				}
			} else {
				eb.setTitle("Seu avatar");
				eb.setImage(author.getEffectiveAvatarUrl() + "?size=4096");
				try {
					eb.setColor(Helper.colorThief(author.getEffectiveAvatarUrl()));
				} catch (IOException ignore) {
				}
			}
		} else if (message.getMentionedUsers().size() == 1) {
			if(author.getId().equals(message.getMentionedUsers().get(0).getId())) {
				eb.setTitle("Seu avatar");
				eb.setImage(author.getEffectiveAvatarUrl() + "?size=4096");
			} else {
				eb.setTitle("Avatar de: " + message.getMentionedUsers().get(0).getAsTag());
				eb.setImage(message.getMentionedUsers().get(0).getEffectiveAvatarUrl() + "?size=4096");
				try {
					eb.setColor(Helper.colorThief(message.getMentionedUsers().get(0).getEffectiveAvatarUrl()));
				} catch (IOException ignore) {
				}
			}
		}
		channel.sendMessage(eb.build()).queue();
	}
}
