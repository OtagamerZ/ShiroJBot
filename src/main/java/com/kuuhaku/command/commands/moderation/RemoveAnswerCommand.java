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
import com.kuuhaku.controller.sqlite.CustomAnswerDAO;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.NoResultException;

public class RemoveAnswerCommand extends Command {

    public RemoveAnswerCommand() {
        super("nãofale", "<id>", "Remove uma resposta especificada.", Category.MODERACAO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        if (args.length == 0) {
            channel.sendMessage(":x: | Você precisa especificar um ID.").queue();
            return;
        } else if (!StringUtils.isNumeric(args[0])) {
            channel.sendMessage(":x: | O ID deve ser um valor numérico informado na lista de respostas (`" + prefix + "fale lista`).").queue();
            return;
        }

        try {
            CustomAnswerDAO.removeCAFromDB(CustomAnswerDAO.getCAByID(Long.parseLong(args[0])));
            channel.sendMessage("Não vou mais responder com a resposta `" + args[0] + "`.").queue();
        } catch (NoResultException e) {
            channel.sendMessage(":x: | ID de resposta inválido.").queue();
        }
    }
}
