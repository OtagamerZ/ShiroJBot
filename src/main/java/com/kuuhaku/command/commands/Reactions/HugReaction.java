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
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HugReaction extends Reaction {
    private static boolean answer = false;

    public HugReaction(boolean isAnswer) {
        super("abraçar", new String[]{"abracar", "hug", "vemca"},  "Abraça alguém.", Category.FUN);
        answer = isAnswer;
    }

    private static boolean isAnswer() {
        return answer;
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

        if (message.getMentionedUsers().size() == 0) { Helper.typeMessage(channel, ":x: | Epa, você precisa mencionar alguém para abraçar!"); return; }
        if (message.getMentionedUsers().size() > 1) { Helper.typeMessage(channel, ":x: | Você só pode abraçar uma pessoa por vez, vai com calma!"); return;  }

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(ReactionsList.hug()).openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            this.setReaction(new String[]{
                "Awnn...ai qualquer um shippa!",
                "Ai sim ein, vai pra cima garoto(a)!",
                "Já é o primeiro passo!"
            });

            this.setSelfTarget(new String[]{
                "Ow ow ow, sem pegação!",
                "Meu Nii-chan vai ficar bravo com isso!",
                "Moshi moshi, FBI-sama?"
            });

            if (!isAnswer()) Helper.sendReaction(channel, author.getAsMention() + " abraçou " + message.getMentionedUsers().get(0).getAsMention() + " - " + this.getReaction()[this.getReactionLength()], con.getInputStream(), false);
            else Helper.sendReaction(channel,  message.getMentionedUsers().get(0).getAsMention() + " devolveu o abraço de " + author.getAsMention() + " - " + this.getReaction()[this.getReactionLength()], con.getInputStream(), true);
        } catch (IOException e) { e.printStackTrace();  }
    }
}
