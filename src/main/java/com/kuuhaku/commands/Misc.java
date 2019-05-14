package com.kuuhaku.commands;

import com.kuuhaku.model.Badges;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.json.JSONException;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class Misc {
    public static void help(MessageReceivedEvent message, String prefix, User owner) {
        message.getAuthor().openPrivateChannel().queue(channel -> {
            channel.sendMessage("__**Precisa de ajuda? Aqui estou eu!**__\n").queue();
            channel.sendMessage(Embeds.helpEmbed(prefix)).queue();
            channel.sendMessage("Precisa de mais ajuda? Fale com meu Nii-chan " + owner.getAsMention() + " ou venha para nosso servidor de suporte: https://discord.gg/HpuF3Vr").queue();
        });
    }

    public static void yesNo(MessageReceivedEvent message) {
        String[] responses = {"Sim", "N\u00e3o", "Nunca", "Sempre", "Talvez", "N\u00e3o sei", "Acho que sim", "Acho que n\u00e3o"};
        message.getChannel().sendMessage(responses[(int) (Math.random() * 9)]).queue();
    }

    public static void choose(MessageReceivedEvent message, String cmd) {
        try {
            message.getChannel().sendMessage("Eu escolho essa opção: " + message.getMessage().getContentRaw().replace(cmd, "").split(";")[(int) (Math.random() * message.getMessage().getContentRaw().split(";").length)].trim()).queue();
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendMessage("Você não me deu opções, bobo!").queue();
        }
    }

    public static void image(MessageReceivedEvent message, String[] cmd) {
        try {
            if (cmd.length >= 3) {
                try {
                    message.getChannel().sendMessage(Embeds.imageEmbed(cmd[1].split(";"), cmd[2])).queue();
                } catch (JSONException e) {
                    message.getChannel().sendMessage("Eita, n\u00e3o encontrei nenhuma imagem com essas tags :expressionless:!").queue();
                }
            } else if (cmd.length == 2) {
                try {
                    message.getChannel().sendMessage(Embeds.imageEmbed(cmd[1].split(";"))).queue();
                } catch (JSONException e) {
                    message.getChannel().sendMessage("Eita, n\u00e3o encontrei nenhuma imagem, tente usar tags mais populares ou especificar um n\u00famero de p\u00e1gina!").queue();
                }
            } else {
                message.getChannel().sendMessage("Voc\u00ea n\u00e3o me disse que tipo de imagem devo procurar.").queue();
            }
        } catch (IOException e) {
            message.getChannel().sendMessage("Eita, n\u00e3o encontrei nenhuma imagem :expressionless:!").queue();
        }
    }

    public static void uptime(MessageReceivedEvent message) {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        message.getChannel().sendMessage("Hummm...acho que estou acordada a " + (int) rb.getUptime() / 1000 + " segundos!").queue();
    }

    public static void badges(MessageReceivedEvent message) {
        try {
            message.getChannel().sendMessage(Badges.getBadgeDesc(message.getMessage().getContentRaw().split(" ")[1])).queue();
        } catch (Exception e) {
            message.getChannel().sendMessage("Digite um Nº de uma conquista válido.").queue();
        }
    }
}
