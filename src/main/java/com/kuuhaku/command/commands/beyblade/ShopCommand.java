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
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.Beyblade;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.util.Objects;

public class ShopCommand extends Command {

    public ShopCommand() {
        super("bshop", new String[]{"bloja"}, "", "Mostra o menu da loja.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (MySQL.getBeybladeById(author.getId()) == null) {
            channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
            return;
        }

        channel.sendMessage(":hourglass: Analizando...").queue(m -> {
            Beyblade bb = Objects.requireNonNull(MySQL.getBeybladeById(author.getId()));
            String prefixo = SQLite.getGuildPrefix(guild.getId());

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.decode(bb.getColor()));
            eb.setAuthor("Saldo de " + message.getAuthor().getName() + ": " + Objects.requireNonNull(MySQL.getBeybladeById(author.getId())).getPoints() + " pontos de combate", null, "https://i.imgur.com/H1KqxRc.png");
            eb.setThumbnail("https://i.pinimg.com/originals/d3/34/db/d334db51554b3c78fbb65d6cf5e0dcef.png");

            if (args.length == 0) {

                eb.setTitle("**Loja**");

                eb.setDescription("Olá, meu nome é Orisha, sou técnica em Beyblades.\n\n" +
                        "Estou vendo que você tem um modelo e tanto aqui ein? Enfim, fique à vontade para escolher um atributo " +
                        "para melhorar, note que cada vez que você melhorar algo o valor das outras peças também irão aumentar um pouco " +
                        "devido a incompatibilidade com peças mais baratas!");

                eb.addField(":new: Melhorar nome (Muda o nome):", "50 pontos de combate\nDiga **" + prefixo + "bshop nome** para comprar", false);
                eb.addField(":punch: Melhorar força (Aumenta o dano):", Math.round(15 * bb.getStrength() + bb.getStrength() + bb.getSpeed() + bb.getStability()) + " pontos de combate\nDiga **" + prefixo + "bshop força** para comprar", false);
                eb.addField(":cyclone: Melhorar velocidade (Aumenta a chance de acerto do especial):", Math.round(15 * bb.getSpeed() + bb.getStrength() + bb.getSpeed() + bb.getStability()) + " pontos de combate\nDiga **" + prefixo + "bshop velocidade** para comprar", false);
                eb.addField(":shield: Melhorar estabilidade (Aumenta a defesa):", Math.round(15 * bb.getStability() + bb.getStrength() + bb.getSpeed() + bb.getStability()) + " pontos de combate\nDiga **" + prefixo + "bshop estabilidade** para comprar", false);
                eb.addField(":heart: Melhorar vida (Aumenta a vida):", Math.round(bb.getLife() / 2) + " pontos de combate\nDiga **" + prefixo + "bshop vida** para comprar", false);

                m.delete().queue();
                channel.sendMessage(eb.build()).queue();
                return;
            }

            switch (args[0]) {
                case "nome":
                    if (args.length < 2) {
                        channel.sendMessage(":x: | Você precisa especificar um nome.").queue();
                        return;
                    }
                    changeName(args[1], channel, bb);
                case "força":
                    upgradeStrength(channel, bb);
                    break;
                case "velocidade":
                    upgradeSpeed(channel, bb);
                    break;
                case "estabilidade":
                    upgradeStability(channel, bb);
                    break;
                case "vida":
                    upgradeLife(channel, bb);
                    break;
                default:
                    channel.sendMessage(":x: | Atributo inválido.").queue();
            }
        });
    }

    private static void changeName(String arg, MessageChannel channel, Beyblade bb) {
        if (bb.getPoints() < 50) {
            channel.sendMessage(":x: | Você não possui pontos de combate suficientes.").queue();
            return;
        } else if (arg.length() > 16) {
            channel.sendMessage(":x: | Você escolheu um nome muito longo.").queue();
            return;
        }

        bb.takePoints(50);
        bb.setName(arg);
        MySQL.sendBeybladeToDB(bb);
        channel.sendMessage("Nome trocado com sucesso!").queue();
    }

    private static void upgradeStrength(MessageChannel channel, Beyblade bb) {
        if (bb.getPoints() < Math.round(15 * bb.getStrength() + bb.getStrength() + bb.getSpeed() + bb.getStability())) {
            channel.sendMessage(":x: | Você não possui pontos de combate suficientes.").queue();
            return;
        }

        bb.takePoints(Math.round(15 * bb.getStrength() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
        bb.addStrength();
        MySQL.sendBeybladeToDB(bb);
        channel.sendMessage("Força melhorada com sucesso!").queue();
    }

    private static void upgradeSpeed(MessageChannel channel, Beyblade bb) {
        if (bb.getPoints() < Math.round(15 * bb.getSpeed() + bb.getStrength() + bb.getSpeed() + bb.getStability())) {
            channel.sendMessage(":x: | Você não possui pontos de combate suficientes.").queue();
            return;
        }

        bb.takePoints(Math.round(15 * bb.getSpeed() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
        bb.addSpeed();
        MySQL.sendBeybladeToDB(bb);
        channel.sendMessage("Velocidade melhorada com sucesso!").queue();
    }

    private static void upgradeStability(MessageChannel channel, Beyblade bb) {
        if (bb.getPoints() < Math.round(15 * bb.getStability() + bb.getStrength() + bb.getSpeed() + bb.getStability())) {
            channel.sendMessage(":x: | Você não possui pontos de combate suficientes.").queue();
            return;
        }

        bb.takePoints(Math.round(15 * bb.getStability() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
        bb.addStability();
        MySQL.sendBeybladeToDB(bb);
        channel.sendMessage("Estabilidade melhorada com sucesso!").queue();
    }

    private static void upgradeLife(MessageChannel channel, Beyblade bb) {
        if (bb.getPoints() < Math.round(bb.getLife() / 2)) {
            channel.sendMessage(":x: | Você não possui pontos de combate suficientes.").queue();
            return;
        }

        bb.takePoints(Math.round(bb.getLife() / 2));
        bb.addLife();
        MySQL.sendBeybladeToDB(bb);
        channel.sendMessage("Vida melhorada com sucesso!").queue();
    }
}
