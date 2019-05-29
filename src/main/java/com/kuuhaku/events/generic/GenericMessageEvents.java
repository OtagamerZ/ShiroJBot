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

package com.kuuhaku.events.generic;

import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.persistence.NoResultException;

public class GenericMessageEvents extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        User author = event.getAuthor();
        Member member = event.getMember();
        Message message = event.getMessage();
        MessageChannel channel = message.getChannel();
        Guild guild = message.getGuild();
        String rawMessage = message.getContentRaw();

        String prefix;
        prefix = SQLite.getGuildPrefix(guild.getId());

        if (Main.getInfo().getSelfUser().getIdLong() == author.getIdLong()) return;
        if (author.isBot()) return;

		/*
		if(event.getPrivateChannel()!=null) {
			try {
				Helper.sendPM(author, Helper.formatMessage(Messages.PM_CHANNEL, "help", author));
			} catch (Exception e) {
				DiscordHelper.sendAutoDeleteMessage(channel, YuiHelper.formatMessage(Messages.PM_CHANNEL, "help", author));
			}
			return;
		}

		if(message.getInvites().size()>0 && Helper.getPrivilegeLevel(member) == PrivilegeLevel.USER) {
            message.delete().queue();
            try {
				Helper.sendPM(author, Messages.INVITE_SENT);
            } catch (Exception e) {
				Helper.sendPM(author, ":x: | ");
            }
            return;
        }
		*/

        if (message.getContentRaw().equals(Main.getInfo().getSelfUser().getAsMention())) {
            channel.sendMessage("Para obter ajuda sobre como me utilizar use `" + prefix + "ajuda`.").queue();
            return;
        }

        String rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
        String commandName = rawMsgNoPrefix.split(" ")[0].trim();

        try {
            CustomAnswers ca = SQLite.getCAByTrigger(rawMsgNoPrefix);
            channel.sendMessage(ca.getAnswer()).queue();
        } catch (NoResultException ignore) {
        }

        boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
        String[] args = new String[]{};
        if (hasArgs) {
            args = rawMsgNoPrefix.substring(commandName.length()).trim().split(" ");
        }

        boolean found = false;
        for (Command command : Main.getCommandManager().getCommands()) {
            if (command.getName().equalsIgnoreCase(commandName)) {
                found = true;
            }
            for (String alias : command.getAliases()) {
                if (alias.equalsIgnoreCase(commandName)) {
                    found = true;
                }
            }
            if (!command.getCategory().isEnabled()) {
                found = false;
            }

            if (found) {
                if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
                    channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
                }
                command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
                break;
            }
        }

        if (!found) {
            try {
                com.kuuhaku.model.Member m = SQLite.getMemberById(member.getUser().getId() + member.getGuild().getId());
                boolean lvlUp = m.addXp();
                if (lvlUp) {
                    channel.sendMessage(member.getEffectiveName() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
                }
                SQLite.saveMemberToDB(m);
            } catch (NoResultException e) {
                SQLite.addMemberToDB(member);
            }
        }
    }
}