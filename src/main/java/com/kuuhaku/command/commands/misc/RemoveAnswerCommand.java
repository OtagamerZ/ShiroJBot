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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.persistence.NoResultException;

public class RemoveAnswerCommand extends Command {

    public RemoveAnswerCommand() {
        super("nãofale", "<id>", "Remove uma resposta especificada.", Category.MODERACAO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (args.length == 0) {
            channel.sendMessage(":x: | Você precisa especificar um ID.").queue();
            return;
        }

        try {
            SQLite.removeCAFromDB(SQLite.getCAByID(Long.parseLong(args[0])));
            channel.sendMessage("Não vou mais responder com a resposta `" + args[0] + "`.").queue();
        } catch (NoResultException e) {
            channel.sendMessage(":x: | ID de resposta inválido.").queue();
        }
    }
}
