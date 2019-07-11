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
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class AntispamCommand extends Command {

    public AntispamCommand() {
        super("semspam", new String[]{"nospam", "antispam"}, "<soft/hard>", "Bloqueia ou permite spam no canal onde este comando foi digitado.", Category.MODERACAO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        guildConfig gc = SQLite.getGuildById(guild.getId());

        if (args.length > 0 && (args[0].equalsIgnoreCase("soft") || args[0].equalsIgnoreCase("hard"))) {
            switch (args[0].toLowerCase()) {
                case "soft":
                    gc.setHardAntispam(false);
                    SQLite.updateGuildChannels(gc);
                    return;
                case "hard":
                    gc.setHardAntispam(true);
                    SQLite.updateGuildChannels(gc);
                    return;
                default: channel.sendMessage(":x: | Tipo incorreto, os tipos são `soft` ou `hard`").queue();
            }
            return;
        }

        if (SQLite.getGuildNoSpamChannels(gc.getGuildID()).contains(channel.getId())) gc.removeNoSpamChannel(message.getTextChannel());
        else gc.addNoSpamChannel(message.getTextChannel());

        SQLite.updateGuildChannels(gc);

        channel.sendMessage("Agora spam neste canal está " + (SQLite.getGuildNoLinkChannels(gc.getGuildID()).contains(channel.getId()) ? "**bloqueado**" : "**liberado**")).queue();
    }
}
