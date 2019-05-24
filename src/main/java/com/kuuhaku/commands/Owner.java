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

import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Owner {
    public static void getServers(JDA bot, MessageReceivedEvent message) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> guilds = new ArrayList<>();

        bot.getGuilds().forEach(g -> {
            names.add(g.getName());
            ids.add(g.getId());
        });

        for (int i = 0; i < names.size(); i++) {
            guilds.add(names.get(i) + " | " + ids.get(i));
        }

        if (guilds.isEmpty()) {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Nenhum servidor encontrado").queue());
        } else {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Servidores que participo:\n" + guilds.toString().replace("[", "```").replace("]", "```").replace(", ", "\n")).queue());
        }
    }

    public static void getGuildMap(MessageReceivedEvent message, Map<String, guildConfig> gc) {
        final ArrayList<String> map = new ArrayList<>();
        gc.values().forEach(g -> map.add("```" + g.getGuildId() + " | " + g.getPrefix() + " | " + g.getCanalbv() + " | " + g.getCanalav() + " | " + g.getMsgBoasVindas() + " | " + g.getMsgAdeus() + "```\n"));

        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(map.toString().replace("[", "").replace("]", "").replace(",", "")).queue());
    }

    private static ArrayList<String> getGuilds(JDA bot) throws NullPointerException {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> guilds = new ArrayList<>();

        bot.getGuilds().forEach(g -> names.add(g.getName()));

        for (String name : names) {
            guilds.add(name + " | ");
        }

        if (guilds.isEmpty()) {
            return null;
        } else {
            return guilds;
        }
    }

    public static void broadcast(Map<String, guildConfig> gc, JDA bot, String message, TextChannel homeLog) {
        List<Guild> guilds = bot.getGuilds();
        List<String> status = getGuilds(bot);

        for (int z = 0; z < guilds.size(); z++) {
            try {
                guilds.get(z).getTextChannelById(gc.get(guilds.get(z).getId()).getCanalav()).sendMessage("Transmissão:```" + message + "```").queue();
                assert status != null;
                status.set(z, status.get(z) + "SUCESSO\n");
            } catch (Exception e) {
                System.out.println("Erro: " + e);
                assert status != null;
                status.set(z, status.get(z) + "FALHA\n");
            }
        }
        assert status != null;
        homeLog.sendMessage("Resultado da transmissão:```" + String.join("", status).replace("[", "").replace("]", "\n") + "```").queue();
    }

    public static void listPerms(JDA bot, MessageReceivedEvent message) {
        try {
            Guild guild = bot.getGuildById(message.getMessage().getContentRaw().split(" ")[1]);
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(guild.getName() + " | " + guild.getSelfMember().getPermissions().toString().replace("[", "```").replace("]", "```").replace(", ", "\n")).queue());
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!").queue());
        }
    }

    public static void leave(JDA bot, MessageReceivedEvent message) {
        try {
            Guild guild = bot.getGuildById(message.getMessage().getContentRaw().split(" ")[1]);
            guild.leave().queue();
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Ok, já saí daquele servidor, Nii-chan!").queue());
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!").queue());
        }
    }

    public static Game getRandomGame(JDA bot) {
        ArrayList<Game> games = new ArrayList<>();

        games.add(Game.playing("Digite !ajuda para ver meus comandos!"));
        games.add(Game.streaming("Na conta do meu Nii-chan sem ele saber!", "https://twitch.tv/kuuhaku_otgmz"));
        games.add(Game.playing("Nico nico nii!!"));
        games.add(Game.listening(bot.getGuilds().size() + " servidores, e isso ainda é só o começo!"));
        games.add(Game.watching("No Game No Life pela 13ª vez, e ainda não enjoei de ver como eu atuo bem!"));

        return games.get((int) (Math.random() * games.size()));
    }
}
