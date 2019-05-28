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

package com.kuuhaku.events.guild;

import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.SQLite;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class GuildEvents extends ListenerAdapter {

    @Override//removeGuildFromDB
    public void onGuildJoin(GuildJoinEvent event) {
        SQLite.addGuildToDB(event.getGuild());
        System.out.println("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        guildConfig gc = new guildConfig();
        gc.setGuildId(event.getGuild().getId());
        SQLite.removeGuildFromDB(gc);
        System.out.println("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
    }
}