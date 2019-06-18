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

package com.kuuhaku.command.commands.Reactions;

import com.kuuhaku.command.Category;
import com.kuuhaku.model.ReactionsList;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class NopeReaction extends Reaction {
    public NopeReaction() {
        super("nope", new String[]{"sqn", "hojenão", "esquiva"}, "Evita a tentativa de alguém.", Category.FUN);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(ReactionsList.nope()).openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            this.setReaction(new String[]{
                    "Hoje não!",
                    "Fluido como a água!",
                    "Ha ah, errou!"
            });

            Helper.sendReaction(channel, author.getAsMention() + " esquivou! - " + this.getReaction()[this.getReactionLength()], con.getInputStream(), false);
        } catch (IOException e) {
            Helper.log(this.getClass(), LogLevel.ERROR, e.toString());
        }
    }
}
