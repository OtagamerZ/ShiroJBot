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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

import javax.persistence.NoResultException;

public class KickMemberCommand extends Command {

    public KickMemberCommand() {
        super("kick", new String[]{"expulsar", "remover"}, "<membro> <razão>", "Expulsa o membro especificado.", Category.MODERACAO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (message.getMentionedUsers().size() == 0) {
            channel.sendMessage(":x: | Você precisa mencionar um membro.").queue();
            return;
        } else if (message.getMentionedUsers().size() > 1) {
            channel.sendMessage(":x: | Você mencionou membros demais.").queue();
            return;
        }

        try {
            if (args.length < 2) {
                guild.getController().kick(message.getMentionedMembers().get(0)).queue();
                channel.sendMessage("Membro expulso com sucesso!").queue();
            } else {
                guild.getController().kick(message.getMentionedMembers().get(0), String.join(" ", args).replace(args[0], "").trim()).queue();
                channel.sendMessage("Membro expulso com sucesso!\nRazão:```" + String.join(" ", args).replace(args[0], "").trim() + "```").queue();
            }
        } catch (InsufficientPermissionException e) {
            channel.sendMessage(":x: | Não possuo a permissão para expulsar membros.").queue();
        }
    }
}
