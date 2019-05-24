/*
 * Copyright (C) 2019 Yago Garcia Sanches Gimenez / KuuHaKu
 *
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
 *     along with Shiro J Bot.  If not, see https://www.gnu.org/licenses/
 */

package com.kuuhaku.commands;

import com.kuuhaku.model.Badges;
import com.kuuhaku.model.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.json.JSONException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;

public class Misc {
    public static void help(MessageReceivedEvent message, String prefix, User owner) {
        sendPM(message.getAuthor(), "__**Precisa de ajuda? Aqui estou eu!**__\n\n" + Embeds.helpEmbed(prefix));
        sendPM(message.getAuthor(), Embeds.helpEmbed2(prefix));
        sendPM(message.getAuthor(), Embeds.helpEmbed3(prefix));
        sendPM(message.getAuthor(), "Precisa de mais ajuda? Fale com meu Nii-chan " + owner.getAsMention() + " ou venha para nosso servidor de suporte: https://discord.gg/HpuF3Vr");
    }

    public static void yesNo(MessageReceivedEvent message) {
        String[] responses = {"Sim", "Não", "Nunca", "Sempre", "Talvez", "Não sei", "Acho que sim", "Acho que não"};
        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(responses[(int) (Math.random() * 9)]).queue());
    }

    public static void choose(MessageReceivedEvent message, String cmd) {
        try {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Eu escolho essa opção: " + message.getMessage().getContentRaw().replace(cmd, "").split(";")[(int) (Math.random() * message.getMessage().getContentRaw().split(";").length)].trim()).queue());
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você não me deu opções, bobo!").queue());
        }
    }

    public static void image(MessageReceivedEvent message, String[] cmd) {
        if (cmd.length >= 3) {
            try {
                message.getChannel().sendTyping().queue(tm -> {
                    try {
                        message.getChannel().sendMessage(Embeds.imageEmbed(cmd[1].split(";"), cmd[2])).queue();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (JSONException e) {
                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Eita, não encontrei nenhuma imagem com essas tags :expressionless:!").queue());
            }
        } else if (cmd.length == 2) {
            try {
                message.getChannel().sendTyping().queue(tm -> {
                    try {
                        message.getChannel().sendMessage(Embeds.imageEmbed(cmd[1].split(";"))).queue();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (JSONException e) {
                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Eita, não encontrei nenhuma imagem, tente usar tags mais populares ou especificar um número de página!").queue());
            }
        } else {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você não me disse que tipo de imagem devo procurar.").queue());
        }
    }

    public static void uptime(MessageReceivedEvent message) {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Hummm...acho que estou acordada a " + (int) rb.getUptime() / 1000 + " segundos!").queue());
    }

    public static void badges(MessageReceivedEvent message) {
        try {
            message.getChannel().sendTyping().queue(tm -> {
                try {
                    message.getChannel().sendMessage(Badges.getBadgeDesc(message.getMessage().getContentRaw().split(" ")[1])).queue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Digite um Nº de uma conquista válido.").queue());
        }
    }

    public static void setBg(MessageReceivedEvent message, String[] cmd, Member m) {
        if (cmd.length == 2) {
            try {
                BufferedImage bi = ImageIO.read(new URL(cmd[1]));
                if (bi.getWidth() == 512 && bi.getHeight() == 256) {
                    m.setBg(cmd[1]);
                } else {
                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Esta imagem está em um tamanho incorreto, tenha certeza que ela seja 512x256!").queue());
                }
            } catch (Exception e) {
                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("O link desta imagem não me parece válido, veja bem se digitou tudo corretamente!").queue());
            }
        }
    }

    public static void sendPM(User user, String message) {
        user.openPrivateChannel().queue( (channel) -> channel.sendMessage(message).queue() );
    }

    public static void sendPM(User user, MessageEmbed embed) {
        user.openPrivateChannel().queue( (channel) -> channel.sendMessage(embed).queue() );
    }
}
