package com.rdx.command.commands.owner;

import com.rdx.model.CustomAnswers;
import com.rdx.model.Member;
import com.rdx.model.guildConfig;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
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
            message.getChannel().sendMessage("Nenhum servidor encontrado").queue();
        } else {
            message.getChannel().sendMessage("Servidores que participo:\n" + guilds.toString().replace("[", "```").replace("]", "```").replace(", ", "\n")).queue();
        }
    }

    public static void getGuildMap(MessageReceivedEvent message, Map<String, guildConfig> gc) {
        final ArrayList<String> map = new ArrayList<>();
        gc.values().forEach(g -> map.add("```" + g.getGuildId() + " | " + g.getPrefix() + " | " + g.getCanalbv() + " | " + g.getCanalav() + " | " + g.getMsgBoasVindas() + " | " + g.getMsgAdeus() + "```\n"));

        message.getChannel().sendMessage(map.toString().replace("[", "").replace("]", "").replace(",", "")).queue();
    }

    public static void getMemberMap(MessageReceivedEvent message, Map<String, Member> mm) {
        final ArrayList<String> map = new ArrayList<>();
        mm.values().forEach(g -> map.add("```" + g.getId() + " | " + g.getLevel() + " | " + g.getXp() + " | " + Arrays.toString(g.getBadges()).replace(",", "-").replace("false", "0").replace("true", "1") + " | " + Arrays.toString(g.getWarns()).replace(",", "-") + "```\n"));

        message.getChannel().sendMessage(map.toString().replace("[", "").replace("]", "").replace(",", "")).queue();
    }

    public static void getAnswersMap(MessageReceivedEvent message, List<CustomAnswers> ca) {
        final ArrayList<String> map = new ArrayList<>();
        ca.forEach(a -> map.add("```" + a.getId() + " | " + a.getGuildID() + " | (" + a.getGatilho() + ") > " + a.getAnswer() + "```\n"));

        message.getChannel().sendMessage(map.toString().replace("[", "").replace("]", "").replace(",", "")).queue();
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
            message.getChannel().sendMessage(guild.getName() + " | " + guild.getSelfMember().getPermissions().toString().replace("[", "```").replace("]", "```").replace(", ", "\n")).queue();
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!").queue();
        }
    }

    public static void leave(JDA bot, MessageReceivedEvent message) {
        try {
            Guild guild = bot.getGuildById(message.getMessage().getContentRaw().split(" ")[1]);
            guild.leave().queue();
            message.getChannel().sendMessage("Ok, já saí daquele servidor, Nii-chan!").queue();
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!").queue();
        }
    }
}
