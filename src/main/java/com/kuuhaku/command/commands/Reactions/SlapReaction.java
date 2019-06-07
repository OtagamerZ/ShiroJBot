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

public class SlapReaction extends Reaction {
    private static boolean answer = false;

    public SlapReaction(boolean isAnswer) {
        super("estapear", new String[]{"tapa", "slap", "baka"}, "Dá um tapa em alguém.", Category.FUN);
        answer = isAnswer;
    }

    private static boolean isAnswer() {
        return answer;
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        try {
            if (message.getMentionedUsers().size() > 0) {
                HttpURLConnection con = (HttpURLConnection) new URL(ReactionsList.slap()).openConnection();
                con.setRequestProperty("User-Agent", "Mozilla/5.0");

                this.setReaction(new String[]{
                        "Kono BAKA!",
                        "Eu faria o mesmo!",
                        "Bem que mereceu!"
                });

                this.setSelfTarget(new String[]{
                        "Fui treinada por meu Nii-chan, POR MEU NII-CHAN!",
                        "Você não pode acertar quem você não vê!",
                        "O que você achou que ia acontecer?!"
                });

                if (message.getMentionedUsers().get(0) == Main.getInfo().getAPI().getSelfUser()) {
                    Helper.sendReaction(channel, author.getAsMention() + " errou o tapa na " + Main.getInfo().getAPI().getSelfUser().getAsMention() + " - " + this.getSelfTarget()[this.getSelfTargetLength()], con.getInputStream(), false);
                    return;
                }
                if (!isAnswer())
                    Helper.sendReaction(channel, author.getAsMention() + " deu um tapa em " + message.getMentionedUsers().get(0).getAsMention() + " - " + this.getReaction()[this.getReactionLength()], con.getInputStream(), true);
                else
                    Helper.sendReaction(channel, message.getMentionedUsers().get(0).getAsMention() + " devolveu o tapa de " + author.getAsMention() + " - " + this.getReaction()[this.getReactionLength()], con.getInputStream(), false);
            } else {
                Helper.typeMessage(channel, ":x: | Epa, você precisa mencionar alguém para dar um tapa!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
