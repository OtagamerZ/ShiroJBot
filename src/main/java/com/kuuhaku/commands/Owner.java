package com.kuuhaku.commands;

import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
            return String.join("\n", "```", guilds.toString().replace("[", "").replace("]", ""), "```");
        }
    }

    public static void getMap(MessageReceivedEvent message, Map<String, guildConfig> gc) {
        final ArrayList<String> map = new ArrayList<>();
        gc.values().forEach(g -> map.add(g.getGuildId() + " | " + g.getPrefix() + " | " + g.getCanalbv() + " | " + g.getCanalav() + " | " + g.getMsgBoasVindas(null) + " | " + g.getMsgAdeus(null) + "\n"));

        message.getChannel().sendMessage("```" + map + "```").queue();
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

    public static void broadcast(JDA bot, String message, TextChannel homeLog) {
        List<Guild> guilds = bot.getGuilds();
        List<String> status = getGuilds(bot);

        for (int z = 0; z < guilds.size(); z++) {
            try {
                if (!guilds.get(z).getTextChannelsByName("avisos-shiro", true).isEmpty()) {
                    guilds.get(z).getTextChannelsByName("avisos-shiro", true).get(0).sendMessage("Transmissão:```" + message + "```").queue();
                } else {
                    for (int i = 0; i < guilds.get(z).getTextChannels().size(); i++) {
                        if (guilds.get(z).getTextChannels().get(i).canTalk()) {
                            guilds.get(z).getTextChannels().get(i).sendMessage("Canal `avisos-shiro` não foi encontrado, enviando transmissão para primeiro canal disponível:```" + message + "```").queue();
                            break;
                        }
                    }
                }
                assert status != null;
                status.set(z, status.get(z) + "SUCESSO\n");
            } catch (Exception e) {
                System.out.println("Erro: " + e);
                assert status != null;
                status.set(z, status.get(z) + "FALHA\n");
            } finally {
                assert status != null;
                homeLog.sendMessage("Resultado da transmissão:```" + String.join("", status).replace("[", "").replace("]", "\n") + "```").queue();
            }
        }
    }

    public static String listPerms(Guild guild) {
        return guild.getName() + " | " + guild.getSelfMember().getPermissions().toString().replace("[", "```").replace("]", "```").replace(", ", "\n");
    }

    public static void leave(Guild guild) {
        guild.leave().complete();
    }
}
