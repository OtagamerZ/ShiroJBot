/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import net.dv8tion.jda.api.entities.*;

public class NoLinkCommand extends Command {

    public NoLinkCommand() {
        super("semlink", new String[]{"nolink", "blocklink"}, "Bloqueia ou permite links postados no canal onde este comando foi digitado.", Category.MODERACAO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        GuildConfig gc = GuildDAO.getGuildById(guild.getId());

        if (gc.getNoLinkChannels().contains(channel.getId())) gc.removeNoLinkChannel(message.getTextChannel());
        else gc.addNoLinkChannel(message.getTextChannel());

        GuildDAO.updateGuildSettings(gc);

        channel.sendMessage("Agora os links neste canal estão " + (gc.getNoLinkChannels().contains(channel.getId()) ? "**bloqueados**" : "**liberados**")).queue();
    }
}
