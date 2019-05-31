package com.kuuhaku.utils;

import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.IOException;

public class Settings {

    public static void embedConfig(Message message) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Helper.colorThief(message.getGuild().getIconUrl()));
        eb.setThumbnail(message.getGuild().getIconUrl());
        eb.setTitle("⚙ | Configurações do servidor");
        eb.addField("\uD83D\uDD17 » Prefixo: __" + SQLite.getGuildPrefix(message.getGuild().getId()) + "__", Helper.VOID, false);
        eb.addBlankField(true); eb.addBlankField(true);
        eb.addField("\uD83D\uDCD6 » Canal de Boas-vindas", SQLite.getGuildCanalBV(message.getGuild().getId()), false);
        eb.addField("\uD83D\uDCDD » Mensagem de Boas-vindas", SQLite.getGuildMsgBV(message.getGuild().getId()), false);
        eb.addField("\uD83D\uDCD6 » Canal de Adeus", SQLite.getGuildCanalAdeus(message.getGuild().getId()), false);
        eb.addField("\uD83D\uDCDD » Mensagem de Adeus", SQLite.getGuildMsgAdeus(message.getGuild().getId()), false);
        eb.addBlankField(true); eb.addBlankField(true); eb.addBlankField(true);
        eb.addField("\uD83D\uDCD6 » Canal de Sugestões", SQLite.getGuildCanalSUG(message.getGuild().getId()), true);
        eb.addField("\uD83D\uDCD6 » Canal de Avisos/Logs", SQLite.getGuildCanalAvisos(message.getGuild().getId()), true);
        eb.addField("\uD83D\uDCD1 » Cargo de Avisos/Warnings", SQLite.getGuildCargoWarn(message.getGuild().getId()), false);

        eb.setFooter("Para obter ajuda sobre como configurar o seu servidor, faça: " + SQLite.getGuildPrefix(message.getGuild().getId()) +  "settings ajuda", null);

        message.getTextChannel().sendMessage(eb.build()).queue();
    }

    public static void updatePrefix(String[] args, Message message, guildConfig gc) {
        if(args.length < 2) { message.getTextChannel().sendMessage("O prefixo atual deste servidor é `" + SQLite.getGuildPrefix(message.getGuild().getId()) + "`.").queue(); return; }

        String newPrefix = args[1].trim();
        if(newPrefix.length() > 5) { message.getTextChannel().sendMessage(":x: | O prefixo `" + newPrefix + "` contem mais de 5 carateres, não pode.").queue(); return; }

        SQLite.updateGuildPrefix(newPrefix, gc);
        message.getTextChannel().sendMessage("✅ | O prefixo deste servidor foi trocado para `" + newPrefix + "` com sucesso.").queue();
    }

    public static void updateCanalBV(String[] args, Message message, guildConfig gc) {
        String antigoCanalBVID = SQLite.getGuildCanalBV(message.getGuild().getId());

        if(args.length < 2) {
            if(antigoCanalBVID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de adeus atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de adeus atual do servidor é <#" + antigoCanalBVID + ">.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue(); return; }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildCanalBV(null, gc);
            message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalBV = message.getMentionedChannels().get(0);

        SQLite.updateGuildCanalBV(newCanalBV.getId(), gc);
        message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi trocado para " + newCanalBV.getAsMention() + " com sucesso.").queue();
    }

    public static void updateCanalAdeus(String[] args, Message message, guildConfig gc) {
        String antigoCanalAdeusID = SQLite.getGuildCanalAdeus(message.getGuild().getId());

        if(args.length < 2) {
            if(antigoCanalAdeusID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de adeus atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de adeus atual do servidor é <#" + antigoCanalAdeusID + ">.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue(); return; }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildCanalAdeus(null, gc);
            message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalAdeus = message.getMentionedChannels().get(0);

        SQLite.updateGuildCanalAdeus(newCanalAdeus.getId(), gc);
        message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi trocado para " + newCanalAdeus.getAsMention() + " com sucesso.").queue();
    }

    public static void updateCanalSUG(String[] args, Message message, guildConfig gc) {
        String antigoCanalSUGID = SQLite.getGuildCanalSUG(message.getGuild().getId());

        if(args.length < 2) {
            if(antigoCanalSUGID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de sugestões atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de sugestões atual do servidor é <#" + antigoCanalSUGID + ">.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue(); return; }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildCanalSUG(null, gc);
            message.getTextChannel().sendMessage("✅ | O canal de sugestões do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalSUG = message.getMentionedChannels().get(0);

        SQLite.updateGuildCanalSUG(newCanalSUG.getId(), gc);
        message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi trocado para " + newCanalSUG.getAsMention() + " com sucesso.").queue();
    }

    public static void updateCanalAvisos(String[] args, Message message, guildConfig gc) {
        String antigoCanalAvisosID = SQLite.getGuildCanalAvisos(message.getGuild().getId());

        if(args.length < 2) {
            if(antigoCanalAvisosID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de avisos/logs atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de avisos/logs atual do servidor é <#" + antigoCanalAvisosID + ">.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue(); return; }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildCanalAvisos(null, gc);
            message.getTextChannel().sendMessage("✅ | O canal de avisos/logs do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalAvisos = message.getMentionedChannels().get(0);

        SQLite.updateGuildCanalAvisos(newCanalAvisos.getId(), gc);
        message.getTextChannel().sendMessage("✅ | O canal de avisos/logs do servidor foi trocado para " + newCanalAvisos.getAsMention() + " com sucesso.").queue();
    }

    public static void updateCargoWarn(String[] args, Message message, guildConfig gc) {
        String antigoCargoWarn = SQLite.getGuildCargoWarn(message.getGuild().getId());

        if(args.length < 2) {
            if(antigoCargoWarn.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O cargo de warns atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O cargo de warns atual do servidor é `" + antigoCargoWarn + "`.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 `cargo.").queue(); return; }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildCargoWarn(null, gc);
            message.getTextChannel().sendMessage("✅ | O cargo de warns do servidor foi resetado com sucesso.").queue();
            return;
        }

        Role newRoleWarns = message.getMentionedRoles().get(0);

        SQLite.updateGuildCargoWarn(newRoleWarns.getId(), gc);
        message.getTextChannel().sendMessage("✅ | O cargo de warns do servidor foi trocado para " + newRoleWarns.getAsMention() + " com sucesso.").queue();
    }
}
