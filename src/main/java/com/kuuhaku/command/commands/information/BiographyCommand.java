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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class BiographyCommand extends Command {

    public BiographyCommand() {
        super("bio", new String[]{"story", "desc"}, "<mensagem>", "Muda a biografia do seu perfil.", Category.MISC);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (String.join(" ", args).length() > 140) {
            channel.sendMessage(":x: | A biografia é muito grande, o tamanho máximo é 140 caractéres.").queue();
            return;
        }

        SQLite.getMemberById(author.getId() + guild.getId()).setBio(String.join(" ", args));
        channel.sendMessage("Biografia definida com sucesso!").queue();
    }
}
