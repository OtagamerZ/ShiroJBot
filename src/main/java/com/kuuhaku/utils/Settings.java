package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.IOException;

public class Settings {

    public static void embedConfig(Message message) throws IOException {
        String prefix = SQLite.getGuildPrefix(message.getGuild().getId());

        String canalBV = SQLite.getGuildCanalBV(message.getGuild().getId());
        if(!canalBV.equals("Não definido.")) canalBV = "<#" + canalBV + ">";
        String msgBV = SQLite.getGuildMsgBV(message.getGuild().getId());
        if(!msgBV.equals("Não definido.")) msgBV = "`" + msgBV + "`";

        String canalAdeus = SQLite.getGuildCanalAdeus(message.getGuild().getId());
        if(!canalAdeus.equals("Não definido.")) canalAdeus = "<#" + canalAdeus + ">";
        String msgAdeus = SQLite.getGuildMsgAdeus(message.getGuild().getId());
        if(!msgAdeus.equals("Não definido.")) msgAdeus = "`" + msgAdeus + "`";

        String canalSUG = SQLite.getGuildCanalSUG(message.getGuild().getId());
        if(!canalSUG.equals("Não definido.")) canalSUG = "<#" + canalSUG + ">";

        String canalAvisos = SQLite.getGuildCanalAvisos(message.getGuild().getId());
        if(!canalAvisos.equals("Não definido.")) canalAvisos = "<#" + canalAvisos + ">";

        String canalLvlUpNotif = SQLite.getGuildCanalLvlUp(message.getGuild().getId());
        if(!canalLvlUpNotif.equals("Não definido.")) canalLvlUpNotif = "<#" + canalLvlUpNotif + ">";

        String canalRelay = SQLite.getGuildCanalRelay(message.getGuild().getId());
        if(!canalRelay.equals("Não definido.")) canalRelay = "<#" + canalRelay + ">";

        String cargoWarnID = SQLite.getGuildCargoWarn(message.getGuild().getId());
        //String cargoNewID = SQLite.getGuildCargoNew(message.getGuild().getId());

        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Helper.colorThief(message.getGuild().getIconUrl()));
        eb.setThumbnail(message.getGuild().getIconUrl());
        eb.setTitle("⚙ | Configurações do servidor");
        eb.setDescription(Helper.VOID);
        eb.addField("\uD83D\uDD17 » Prefixo: __" + prefix + "__", Helper.VOID, false);
        eb.addField("\uD83D\uDCD6 » Canal de Boas-vindas", canalBV, false);
        eb.addField("\uD83D\uDCDD » Mensagem de Boas-vindas", msgBV, false);
        eb.addField(Helper.VOID + "\n" + "\uD83D\uDCD6 » Canal de Adeus", canalAdeus, false);
        eb.addField("\uD83D\uDCDD » Mensagem de Adeus", msgAdeus, false);
        eb.addBlankField(true); eb.addBlankField(true);
        eb.addField("\uD83D\uDCD6 » Canal de Sugestões", canalSUG, true);
        eb.addField("\uD83D\uDCD6 » Canal de Avisos/Logs", canalAvisos, true);
        eb.addField("\uD83D\uDCD6 » Canal Relay", canalRelay, true);

        if (!cargoWarnID.equals("Não definido.")) {
            eb.addField("\uD83D\uDCD1 » Cargo de Avisos/Warns", Main.getInfo().getRoleByID(cargoWarnID).getAsMention(), true);
        }
        else { eb.addField("\uD83D\uDCD1 » Cargo de Avisos/Warns", cargoWarnID, true); }

        //if(!cargoNewID.equals("Não definido.")) { eb.addField("\uD83D\uDCD1 » Cargo automático", com.kuuhaku.Main.getInfo().getRoleByID(cargoNewID).getAsMention(), false); }
        //else { eb.addField("\uD83D\uDCD1 » Cargos automáticos", cargoNewID, true); }

        eb.addField("\uD83D\uDCD6 » Canal de notificação de level up", canalLvlUpNotif, true);


        eb.setFooter("Para obter ajuda sobre como configurar o seu servidor, use " + SQLite.getGuildPrefix(message.getGuild().getId()) + "settings ajuda", null);

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
                message.getTextChannel().sendMessage("O canal de boas-vindas atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de boas-vindas atual do servidor é <#" + antigoCanalBVID + ">.").queue();
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
    public static void updateMsgBV(String[] args, Message message, guildConfig gc) {
        String antigaMsgBV = SQLite.getGuildMsgBV(message.getGuild().getId());

        if(args.length < 2) {
                message.getTextChannel().sendMessage("A mensagem de boas-vindas atual do servidor é `" + antigaMsgBV + "`.").queue();
            return;
        }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildMsgBV("Seja bem-vindo(a) ao %guild%, %user%!", gc);
            message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi resetado com sucesso.").queue();
            return;
        }

        String newMsgBv = String.join(" ", args).replace(args[0], "").replace(args[1], "").trim();

        SQLite.updateGuildMsgBV(newMsgBv, gc);
        message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi trocado para " + newMsgBv + " com sucesso.").queue();
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
    public static void updateMsgAdeus(String[] args, Message message, guildConfig gc) {
        String antigaMsgAdeus = SQLite.getGuildMsgAdeus(message.getGuild().getId());

        if(args.length < 2) {
            message.getTextChannel().sendMessage("A mensagem de boas-vindas atual do servidor é `" + antigaMsgAdeus + "`.").queue();
            return;
        }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildMsgAdeus("Seja bem-vindo(a) ao %guild%, %user%!", gc);
            message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi resetado com sucesso.").queue();
            return;
        }

        String newMsgAdeus = String.join(" ", args).replace(args[0], "").replace(args[1], "").trim();

        SQLite.updateGuildMsgAdeus(newMsgAdeus, gc);
        message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi trocado para " + newMsgAdeus + " com sucesso.").queue();
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

    /*
    public static void updateCargoNew(String[] args, Message message, guildConfig gc) {
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
    */

    public static void updateLevelNotif(String[] args, Message message, guildConfig gc) {
        Boolean LevelUpNotif = SQLite.getGuildLvlUpNotif(message.getGuild().getId());

        if(args.length < 2) {
            if(LevelUpNotif) {
                message.getTextChannel().sendMessage("As mensagens quando alguém sobe de nível estão ativas.").queue();
            } else {
                message.getTextChannel().sendMessage("As mensagens quando alguém sobe de nível não estão ativas.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue(); return; }
        if(args[1].equals("ativar") || args[1].equals("sim")) {
            SQLite.updateGuildLvlUpNotif(true, gc);
            message.getTextChannel().sendMessage("✅ | As mensagens quando alguém sobe de nível foram ativadas com sucesso.").queue();
        } else if(args[1].equals("desativar") || args[1].equals("nao") || args[1].equals("não")) {
            SQLite.updateGuildLvlUpNotif(false, gc);
            message.getTextChannel().sendMessage("✅ | As mensagens quando alguém sobe de nível foram desativadas com sucesso.").queue();
        } else {
            message.getTextChannel().sendMessage(":x: | \"" + args[1] + "\" não é uma opção válida, por favor escolha \"ativar\" ou então \"desativar\".").queue();
        }
    }

    public static void updateCanalLevelUp(String[] args, Message message, guildConfig gc) {
        String antigoCanalLvlUpID = SQLite.getGuildCanalAvisos(message.getGuild().getId());

        if(args.length < 2) {
            if(antigoCanalLvlUpID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de level up atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de level up atual do servidor é <#" + antigoCanalLvlUpID + ">.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue(); return; }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildCanalLvlUp(null, gc);
            message.getTextChannel().sendMessage("✅ | O canal de level up do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalLvlUp = message.getMentionedChannels().get(0);

        SQLite.updateGuildCanalLvlUp(newCanalLvlUp.getId(), gc);
        message.getTextChannel().sendMessage("✅ | O canal de level up do servidor foi trocado para " + newCanalLvlUp.getAsMention() + " com sucesso.").queue();
    }

    public static void updateCanalRelay(String[] args, Message message, guildConfig gc) {
        String antigoCanalRelayID = SQLite.getGuildCanalRelay(message.getGuild().getId());

        if(args.length < 2) {
            if(antigoCanalRelayID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal relay atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal relay atual do servidor é <#" + antigoCanalRelayID + ">.").queue();
            }
            return;
        }
        if(message.getMentionedChannels().size() > 1) { message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue(); return; }
        if(args[1].equals("reset") || args[1].equals("resetar")) {
            SQLite.updateGuildCanalRelay(null, gc);
            message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalRelay = message.getMentionedChannels().get(0);

        SQLite.updateGuildCanalRelay(newCanalRelay.getId(), gc);
        message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi trocado para " + newCanalRelay.getAsMention() + " com sucesso.").queue();
    }
}
