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

package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Beyblade;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SlotsCommand extends Command {

    public SlotsCommand() {
        super("bslots", new String[]{"bcassino", "bapostar"}, "Aposta uma quantidade de pontos de combate no cassino, podendo ganhar ou perder.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (MySQL.getBeybladeById(author.getId()) == null) {
            channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
            return;
        } else if (args.length < 1) {
            channel.sendMessage(":x: | Você precisa especificar uma quantidade de pontos para apostar.").queue();
            return;
        }

        Beyblade bb = Objects.requireNonNull(MySQL.getBeybladeById(author.getId()));
        try {
            final int[] aposta = {Integer.parseInt(args[0])};
            if (aposta[0] > bb.getPoints()) {
                channel.sendMessage(":x: | Você não possui pontos de combate suficientes.").queue();
                return;
            } else if (aposta[0] < 5) {
                channel.sendMessage(":x: | Aposta muito baixa, o valor mínimo é 5.").queue();
                return;
            }
            bb.takePoints(aposta[0]);
            List<String> result = Helper.getGamble();
            int cheese = Collections.frequency(result, ":cheese:");
            int lantern = Collections.frequency(result, ":izakaya_lantern:");
            int money = Collections.frequency(result, ":moneybag:");
            int rosette = Collections.frequency(result, ":rosette:");
            int diamond = Collections.frequency(result, ":diamond_shape_with_a_dot_inside:");

            channel.sendMessage("Aposta de " + author.getAsMention() + ": :diamond_shape_with_a_dot_inside: " + aposta[0] + " pontos.").queue(m -> {
                switch (cheese) {
                    case 3:
                        aposta[0] += Math.round((float) aposta[0] * 0.8f);
                        break;
                    case 4:
                        aposta[0] += Math.round((float) aposta[0] * 0.6f);
                        break;
                    case 5:
                        aposta[0] += Math.round((float) aposta[0] * 0.4f);
                        break;
                }
                switch (lantern) {
                    case 3:
                        aposta[0] += Math.round((float) aposta[0] * 1.3f);
                        break;
                    case 4:
                        aposta[0] += Math.round((float) aposta[0] * 1.6f);
                        break;
                    case 5:
                        aposta[0] += Math.round((float) aposta[0] * 1.9f);
                        break;
                }
                switch (money) {
                    case 3:
                        aposta[0] += Math.round((float) aposta[0] * 1.4f);
                        break;
                    case 4:
                        aposta[0] += Math.round((float) aposta[0] * 1.8f);
                        break;
                    case 5:
                        aposta[0] += Math.round((float) aposta[0] * 2.2f);
                        break;
                }
                switch (rosette) {
                    case 3:
                        aposta[0] += Math.round((float) aposta[0] * 1.5f);
                        break;
                    case 4:
                        aposta[0] += Math.round((float) aposta[0] * 2.0f);
                        break;
                    case 5:
                        aposta[0] += Math.round((float) aposta[0] * 2.5f);
                        break;
                }
                switch (diamond) {
                    case 3:
                        aposta[0] += aposta[0] * 2;
                        break;
                    case 4:
                        aposta[0] += aposta[0] * 3;
                        break;
                    case 5:
                        aposta[0] += aposta[0] * 4;
                        break;
                }

                String res = String.join(" | ", result.toArray(new String[0]));
                int pointWin = aposta[0] - Integer.parseInt(args[0]);
                boolean poorMan = false;
                bb.addPoints(pointWin);
                MySQL.sendBeybladeToDB(bb);

                if (aposta[0] > 15 && pointWin == 0) {
                    pointWin = (Math.round((float) aposta[0] / 5.0f));
                    poorMan = true;
                }

                m.editMessage(m.getContentRaw() + "\n\n| " + res + "|\n\nVocê ganhou " + (poorMan ? pointWin + " pontos! (seguro de pontos)" : pointWin + " pontos!")).queueAfter(2, TimeUnit.SECONDS);
            });
        } catch (NumberFormatException e) {
            channel.sendMessage(":x: | Valor de aposta inválido, por favor utilize apenas números inteiros.").queue();
        }
    }
}
