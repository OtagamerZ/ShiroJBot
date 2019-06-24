/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Tags;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.persistence.NoResultException;

public class VerifiedTagCommand extends Command {

	public VerifiedTagCommand() {
		super("switchverified", new String[]{"mudaverificado", "tagverificado", "verificado"}, "<@usuário>", "Define um usuário como verificado ou não.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (message.getMentionedUsers().size() > 0) {
			if (message.getMentionedUsers().size() == 1) {
				try {
					Tags t = MySQL.getTagById(message.getMentionedUsers().get(0).getId());
					if (t.isPartner()) {
						MySQL.removeTagVerified(message.getMentionedUsers().get(0).getId());
						channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " não é mais verificado, perdeu a confiança!").queue();
					} else {
						MySQL.giveTagVerified(message.getMentionedUsers().get(0).getId());
						MySQL.removeTagToxic(message.getMentionedUsers().get(0).getId());
						channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " agora é verificado, te considero alguém confiável!").queue();
					}
				} catch (NoResultException e) {
					MySQL.addUserTagsToDB(message.getMentionedUsers().get(0).getId());
					Tags t = MySQL.getTagById(message.getMentionedUsers().get(0).getId());
					if (t.isPartner()) {
						MySQL.removeTagVerified(message.getMentionedUsers().get(0).getId());
						channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " não é mais verificado, perdeu a confiança!").queue();
					} else {
						MySQL.giveTagVerified(message.getMentionedUsers().get(0).getId());
						MySQL.removeTagToxic(message.getMentionedUsers().get(0).getId());
						channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " agora é verificado, te considero alguém confiável!").queue();
					}
				}
			} else {
				channel.sendMessage(":x: | Nii-chan, você mencionou usuários demais!").queue();
			}
		} else {
			try {
				if (Main.getInfo().getUserByID(args[0]) != null) {
					try {
						Tags t = MySQL.getTagById(args[0]);
						if (t.isPartner()) {
							MySQL.removeTagVerified(args[0]);
							channel.sendMessage("<@" + args[0] + "> não é mais verificado, perdeu a confiança!").queue();
						} else {
							MySQL.giveTagVerified(args[0]);
							MySQL.removeTagToxic(args[0]);
							channel.sendMessage("<@" + args[0] + "> agora é verificado, te considero alguém confiável!").queue();
						}
					} catch (NoResultException e) {
						MySQL.addUserTagsToDB(args[0]);
						Tags t = MySQL.getTagById(args[0]);
						if (t.isPartner()) {
							MySQL.removeTagVerified(args[0]);
							channel.sendMessage("<@" + args[0] + "> não é mais verificado, perdeu a confiança!").queue();
						} else {
							MySQL.giveTagVerified(args[0]);
							MySQL.removeTagToxic(args[0]);
							channel.sendMessage("<@" + args[0] + "> agora é verificado, te considero alguém confiável!").queue();
						}
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				channel.sendMessage(":x: | Nii-chan bobo, você precisa mencionar um usuário!").queue();
			}
		}
	}
}
