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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Tags;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.NoResultException;

@Command(
		name = "verificado",
		aliases = {"verified"},
		usage = "req_mention",
		category = Category.DEV
)
public class VerifiedTagCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() > 0) {
			if (message.getMentionedUsers().size() == 1) {
				try {
					resolveVerifiedByMention(message, channel);
				} catch (NoResultException e) {
					TagDAO.addUserTagsToDB(message.getMentionedUsers().get(0).getId());
					resolveVerifiedByMention(message, channel);
				}
			} else {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_too-many-users-nv")).queue();
			}
		} else {
			try {
				if (Main.getInfo().getUserByID(args[0]) != null) {
					try {
						resolveVerifiedById(args, channel);
					} catch (NoResultException e) {
						TagDAO.addUserTagsToDB(args[0]);
						resolveVerifiedById(args, channel);
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user-nv")).queue();
			}
		}
	}

	private void resolveVerifiedById(String[] args, MessageChannel channel) {
		Tags t = TagDAO.getTagById(args[0]);
		if (t.isVerified()) {
			TagDAO.removeTagVerified(args[0]);
			channel.sendMessage("<@" + args[0] + "> não é mais verificado, perdeu a confiança!").queue();
		} else {
			TagDAO.giveTagVerified(args[0]);
			TagDAO.removeTagToxic(args[0]);
			channel.sendMessage("<@" + args[0] + "> agora é verificado, te considero alguém confiável!").queue();
		}
	}

	private void resolveVerifiedByMention(Message message, MessageChannel channel) {
		Tags t = TagDAO.getTagById(message.getMentionedUsers().get(0).getId());
		if (t.isVerified()) {
			TagDAO.removeTagVerified(message.getMentionedUsers().get(0).getId());
			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " não é mais verificado, perdeu a confiança!").queue();
		} else {
			TagDAO.giveTagVerified(message.getMentionedUsers().get(0).getId());
			TagDAO.removeTagToxic(message.getMentionedUsers().get(0).getId());
			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " agora é verificado, te considero alguém confiável!").queue();
		}
	}
}
