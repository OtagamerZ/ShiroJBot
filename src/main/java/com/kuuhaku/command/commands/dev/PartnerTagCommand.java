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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Tags;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.persistence.NoResultException;

public class PartnerTagCommand extends Command {

    public PartnerTagCommand() {
        super("switchpartner", new String[]{"mudaparceiro", "tagparceiro", "éparça"}, "<@usuário>", "Define um usuário como parceiro ou não.", Category.DEVS);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (message.getMentionedUsers().size() > 0) {
            if (message.getMentionedUsers().size() == 1) {
                try {
                    Tags t = MySQL.getTagById(message.getMentionedMembers().get(0).getUser().getId());
                    if (t.isPartner()) {
                        MySQL.removeTagPartner(message.getMentionedMembers().get(0));
                        channel.sendMessage(message.getMentionedMembers().get(0).getAsMention() + " não é mais parceiro, foi bom enquanto durou!").queue();
                    } else {
                        MySQL.giveTagPartner(message.getMentionedMembers().get(0));
                        channel.sendMessage(message.getMentionedMembers().get(0).getAsMention() + " agora é um parceiro, que iniciem os negócios!").queue();
                    }
                } catch (NoResultException e) {
                    MySQL.addUserTagsToDB(message.getMentionedMembers().get(0));
                    Tags t = MySQL.getTagById(message.getMentionedMembers().get(0).getUser().getId());
                    if (t.isPartner()) {
                        MySQL.removeTagPartner(message.getMentionedMembers().get(0));
                        channel.sendMessage(message.getMentionedMembers().get(0).getAsMention() + " não é mais parceiro, foi bom enquanto durou!").queue();
                    } else {
                        MySQL.giveTagPartner(message.getMentionedMembers().get(0));
                        channel.sendMessage(message.getMentionedMembers().get(0).getAsMention() + " agora é um parceiro, que iniciem os negócios!").queue();
                    }
                }
            } else {
                channel.sendMessage(":x: | Nii-chan, você mencionou usuários demais!").queue();
            }
        } else {
            channel.sendMessage(":x: | Nii-chan bobo, você precisa mencionar um usuário!").queue();
        }
    }
}
