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
import com.kuuhaku.controller.SQLiteOld;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

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

        for (String s : args) if (s.length() > 29) {
            channel.sendMessage(":x: | A biografia possui uma ou mais falavras MUITO grandes (limite de 29 caractéres por palavra).").queue();
            return;
        }

        com.kuuhaku.model.Member m = SQLiteOld.getMemberById(author.getId() + guild.getId());
        m.setBio(String.join(" ", args));
        SQLiteOld.updateMemberSettings(m);
        channel.sendMessage("Biografia definida com sucesso!").queue();
    }
}
