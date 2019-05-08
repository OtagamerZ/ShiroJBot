package com.kuuhaku.commands;

import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Admin {
    public static void config(String[] cmd, MessageReceivedEvent message, guildConfig gc) {
        try {
            switch (cmd[1]) {
                case "canalbv":
                    try {
                        gc.setCanalbv(message.getMessage().getMentionedChannels().get(0));
                        message.getChannel().sendMessage("Canal de boas-vindas trocado para " + gc.getCanalbv().getAsMention()).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("E qual canal devo usar para mensagens de boas-vindas? N\u00falo n\u00e3o \u00e9 um canal v\u00e1lido!").queue();
                    }
                    break;
                case "canalav":
                    try {
                        gc.setCanalav(message.getMessage().getMentionedChannels().get(0));
                        message.getChannel().sendMessage("Canal de avisos trocado para " + gc.getCanalav().getAsMention()).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("E qual canal devo usar para mensagens de aviso? N\u00falo n\u00e3o \u00e9 um canal v\u00e1lido!").queue();
                    }
                    break;
                case "prefixo":
                    try {
                        gc.setPrefix(cmd[2]);
                        message.getChannel().sendMessage("Prefixo trocado para __**" + gc.getPrefix() + "**__").queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Faltou me dizer o prefixo, bobo!").queue();
                    }
                    break;
                case "msgbv":
                    try {
                        if (cmd[2].contains("\"")) {
                            gc.setMsgBoasVindas(String.join("", message.getMessage().getContentRaw().split(gc.getPrefix() + "definir msgbv")));
                            message.getChannel().sendMessage("Agora irei dizer __**" + gc.getMsgBoasVindas(null) + "**__ para usu\u00e1rios que entrarem no servidor!").queue();
                        } else {
                            message.getChannel().sendMessage("A mensagem deve estar entre aspas (\")").queue();
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Voc\u00ea n\u00e3o me disse que mensagem devo dizer quando algu\u00e9m entrar!").queue();
                    }
                    break;
                case "msgadeus":
                    try {
                        if (cmd[2].contains("\"")) {
                            gc.setMsgAdeus(String.join("", message.getMessage().getContentRaw().split(gc.getPrefix() + "definir msgadeus")));
                            message.getChannel().sendMessage("Agora irei dizer __**" + gc.getMsgAdeus(null) + "**__ para membros que deixarem o servidor!").queue();
                        } else {
                            message.getChannel().sendMessage("A mensagem deve estar entre aspas (\")").queue();
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Voc\u00ea n\u00e3o me disse que mensagem devo dizer quando algu\u00e9m sair!").queue();
                    }
                    break;
                default:
                    message.getChannel().sendMessage("N\u00e3o conhe\u00e7o esse comando, certeza que digitou corretamente?").queue();
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendMessage("Voc\u00ea precisa me dizer o qu\u00ea devo definir").queue();
        }
    }
}
