package com.kuuhaku.commands;

import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Owner {
    public static String getServers(JDA bot) {
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
            return "Nenhum servidor encontrado";
        } else {
            return guilds.toString().replace("[", "```").replace("]", "```").replace(", ", "\n");
        }
    }

    public static void getMap(MessageReceivedEvent message, Map<String, guildConfig> gc) {
        final ArrayList<String> map = new ArrayList<>();
        gc.values().forEach(g -> map.add(g.getGuildId() + " | " + g.getPrefix() + " | " + g.getCanalbv() + " | " + g.getCanalav() + " | " + g.getMsgBoasVindas(null) + " | " + g.getMsgAdeus(null) + "\n"));

        message.getChannel().sendMessage(map.toString().replace("[", "```").replace("]", "```").replace(", ", "\n")).queue();
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
            } catch (PermissionException e) {
                System.out.println("Erro: " + e);
                assert status != null;
                status.set(z, status.get(z) + "FALHA\n");
            }
        }
        assert status != null;
        homeLog.sendMessage("Resultado da transmissão:```" + String.join("", status).replace("[", "").replace("]", "\n") + "```").queue();
    }

    public static String listPerms(Guild guild) {
        return guild.getName() + " | " + guild.getSelfMember().getPermissions().toString().replace("[", "```").replace("]", "```").replace(", ", "\n");
    }

    public static void leave(Guild guild) {
        guild.leave().complete();
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
