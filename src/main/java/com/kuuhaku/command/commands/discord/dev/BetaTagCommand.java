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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Tags;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.persistence.NoResultException;

public class BetaTagCommand extends Command {

	public BetaTagCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BetaTagCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BetaTagCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BetaTagCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

    @Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() > 0) {
			if (message.getMentionedUsers().size() == 1) {
				try {
					resolveBetaByMention(message, channel);
				} catch (NoResultException e) {
					TagDAO.addUserTagsToDB(message.getMentionedUsers().get(0).getId());
					resolveBetaByMention(message, channel);
				}
			} else {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_too-many-users-nv")).queue();
            }
        } else {
            try {
                if (Main.getInfo().getUserByID(args[0]) != null) {
                    try {
						resolveBetaById(args, channel);
                    } catch (NoResultException e) {
						TagDAO.addUserTagsToDB(args[0]);
						resolveBetaById(args, channel);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user-nv")).queue();
            }
        }
    }

	private void resolveBetaById(String[] args, MessageChannel channel) {
		Tags t = TagDAO.getTagById(args[0]);
		if (t.isBeta()) {
			TagDAO.removeTagBeta(args[0]);
			channel.sendMessage("<@" + args[0] + "> não possui mais acesso beta!").queue();
		} else {
			TagDAO.giveTagBeta(args[0]);
			channel.sendMessage("<@" + args[0] + "> agora possui acesso beta!").queue();
		}
	}

	private void resolveBetaByMention(Message message, MessageChannel channel) {
		Tags t = TagDAO.getTagById(message.getMentionedUsers().get(0).getId());
		if (t.isBeta()) {
			TagDAO.removeTagBeta(message.getMentionedUsers().get(0).getId());
			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " não possui mais acesso beta!").queue();
		} else {
			TagDAO.giveTagBeta(message.getMentionedUsers().get(0).getId());
			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " agora possui acesso beta!").queue();
		}
	}
}
