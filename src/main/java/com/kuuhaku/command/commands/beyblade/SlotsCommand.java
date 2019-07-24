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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SlotsCommand extends Command {

    public SlotsCommand() {
        super("bslots", new String[]{"bcassino", "bapostar"}, "<valor/slots>", "Aposta uma quantidade de pontos de combate no cassino, podendo ganhar ou perder.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (MySQL.getBeybladeById(author.getId()) == null) {
            channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
            return;
        } else if (args.length < 1) {
            channel.sendMessage(":x: | Você precisa especificar uma quantidade de pontos para apostar ou digitar `s!bapostar slots` para ver as premiações.").queue();
            return;
        } else if (args[0].equalsIgnoreCase("slots") || args[0].equalsIgnoreCase("premio")) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
            eb.setTitle(":confetti_ball: Premios");
            //":cheese:", ":izakaya_lantern:", ":moneybag:", ":diamond_shape_with_a_dot_inside:", ":rosette:"
            eb.addField(":cheese: Queijo (33% de chance)", "" +
                    "3 - Ganha 80% do valor apostado\n" +
                    "4 - Ganha 60% do valor apostado\n" +
                    "5 - Ganha 40% do valor apostado\n", true);
            eb.addField(":izakaya_lantern: Lanterna (27% de chance)", "" +
                    "3 - Ganha 1.3x o valor apostado\n" +
                    "4 - Ganha 1.6x o valor apostado\n" +
                    "5 - Ganha 1.9x o valor apostado\n", true);
            eb.addField(":moneybag: Saco de dinheiro (20% de chance)", "" +
                    "3 - Ganha 1.4x o valor apostado\n" +
                    "4 - Ganha 1.8x o valor apostado\n" +
                    "5 - Ganha 2.2x o valor apostado\n", true);
            eb.addField(":diamond_shape_with_a_dot_inside: Diamante (13% de chance)", "" +
                    "3 - Ganha 1.5x o valor apostado\n" +
                    "4 - Ganha 2x o valor apostado\n" +
                    "5 - Ganha 2.5x o valor apostado\n", true);
            eb.addField(":rosette: Roseta (7% de chance)", "" +
                    "3 - Ganha 2x o valor apostado\n" +
                    "4 - Ganha 3x o valor apostado\n" +
                    "5 - Ganha 4x o valor apostado\n", true);

            channel.sendMessage(eb.build()).queue();
            return;
        }

        Beyblade bb = Objects.requireNonNull(MySQL.getBeybladeById(author.getId()));
        try {
            final int[] aposta = {Integer.parseInt(args[0])};
            if (aposta[0] > bb.getPoints()) {
                channel.sendMessage(":x: | Você não possui pontos de combate suficientes.").queue();
                return;
            } else if (aposta[0] < 5) {
                channel.sendMessage(":x: | Aposta muito baixa, o valor mínimo são 5 pontos de combate. (25 para ativar o seguro de pontos)").queue();
                return;
            }
            bb.takePoints(aposta[0]);
            List<String> result = Helper.getGamble();
            int cheese = Collections.frequency(result, ":cheese:");
            int lantern = Collections.frequency(result, ":izakaya_lantern:");
            int money = Collections.frequency(result, ":moneybag:");
            int diamond = Collections.frequency(result, ":diamond_shape_with_a_dot_inside:");
            int rosette = Collections.frequency(result, ":rosette:");

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
                switch (diamond) {
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
                switch (rosette) {
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

                if (aposta[0] >= 25 && pointWin == 0) {
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
