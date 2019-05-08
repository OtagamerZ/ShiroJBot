package com.kuuhaku.commands;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;

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
                    guilds.get(z).getTextChannelsByName("avisos-shiro", true).get(0).sendMessage("Transmiss\u00e3o:```" + message + "```").queue();
                } else {
                    for (int i = 0; i < guilds.get(z).getChannels().size(); i++) {
                        if (guilds.get(z).getTextChannels().get(i).canTalk()) {
                            guilds.get(z).getTextChannels().get(i).sendMessage("Canal `avisos-shiro` n\u00e3o encontrado, enviando transmiss\u00e3o para primeiro canal dispon\u00eDvel:```" + message + "```").queue();
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
                homeLog.sendMessage("Resultado da transmiss\u00e3o:```" + String.join("", status).replace("[", "").replace("]", "\n") + "```").queue();
            }
        }
    }
}
