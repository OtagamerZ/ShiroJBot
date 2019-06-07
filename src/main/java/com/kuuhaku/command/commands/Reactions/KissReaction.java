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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.model.ReactionsList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class KissReaction extends Reaction {
    private static boolean answer = false;

    public KissReaction(boolean isAnswer) {
        super("beijar", new String[]{"beijo", "kiss", "smac"}, "Beija alguém.", Category.FUN);
        answer = isAnswer;
    }

    private static boolean isAnswer() {
        return answer;
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        try {
            if (message.getMentionedUsers().size() > 0) {
                HttpURLConnection con = (HttpURLConnection) new URL(ReactionsList.kiss()).openConnection();
                con.setRequestProperty("User-Agent", "Mozilla/5.0");

                this.setReaction(new String[]{
                        "Ow wow, vai com calma pessoal!",
                        "Eu...vou deixar vocês sozinhos!",
                        "Um romance melhor que Crepúsculo!"
                });

                this.setSelfTarget(new String[]{
                        "Eu não, sai, xispa!",
                        "Saaaai, não to afim de você!",
                        "Temos um lolicon-sama aqui!"
                });

                if (message.getMentionedUsers().get(0) == Main.getInfo().getAPI().getSelfUser()) {
                    Helper.sendReaction(channel, author.getAsMention() + " tentou beijar a " + Main.getInfo().getAPI().getSelfUser().getAsMention() + " - " + this.getSelfTarget()[this.getSelfTargetLength()], con.getInputStream(), false);
                    return;
                }

                if (!isAnswer())
                    Helper.sendReaction(channel, author.getAsMention() + " beijou " + message.getMentionedUsers().get(0).getAsMention() + " - " + this.getReaction()[this.getReactionLength()], con.getInputStream(), true);
                else
                    Helper.sendReaction(channel, message.getMentionedUsers().get(0).getAsMention() + " devolveu o beijo de " + author.getAsMention() + " - " + this.getReaction()[this.getReactionLength()], con.getInputStream(), false);
            } else {
                Helper.typeMessage(channel, ":x: | Epa, você precisa mencionar alguém para beijar!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
