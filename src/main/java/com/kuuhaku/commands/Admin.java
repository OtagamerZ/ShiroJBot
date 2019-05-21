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

import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.json.JSONObject;

import java.util.Map;

public class Admin {
    public static void config(String[] cmd, MessageReceivedEvent message, guildConfig gc) {
        try {
            switch (cmd[1]) {
                case "canalbv":
                    try {
                        gc.setCanalbv(message.getMessage().getMentionedChannels().get(0).getId());
                        message.getChannel().sendMessage("Canal de boas-vindas trocado para " + message.getGuild().getTextChannelById(gc.getCanalbv()).getAsMention()).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("E qual canal devo usar para mensagens de boas-vindas? Nulo não é um canal válido!").queue();
                    }
                    break;
                case "canalav":
                    try {
                        gc.setCanalav(message.getMessage().getMentionedChannels().get(0).getId());
                        message.getChannel().sendMessage("Canal de avisos trocado para " + message.getGuild().getTextChannelById(gc.getCanalav()).getAsMention()).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("E qual canal devo usar para mensagens de aviso? Nulo não é um canal válido!").queue();
                    }
                    break;
                case "canaladeus":
                    try {
                        gc.setCanaladeus(message.getMessage().getMentionedChannels().get(0).getId());
                        message.getChannel().sendMessage("Canal de adeus trocado para " + message.getGuild().getTextChannelById(gc.getCanaladeus()).getAsMention()).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("E qual canal devo usar para mensagens de adeus? Nulo não é um canal válido!").queue();
                    }
                    break;
                case "canalsug":
                    try {
                        gc.setCanalsug(message.getMessage().getMentionedChannels().get(0).getId());
                        message.getChannel().sendMessage("Canal de sugestões trocado para " + message.getGuild().getTextChannelById(gc.getCanalsug()).getAsMention()).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("E qual canal devo ficar de olho para sugestões? Nulo não é um canal válido!").queue();
                    }
                    break;
                case "canallvl":
                    try {
                        gc.setCanallvl(message.getMessage().getMentionedChannels().get(0).getId());
                        message.getChannel().sendMessage("Canal de notificações de level trocado para " + message.getGuild().getTextChannelById(gc.getCanallvl()).getAsMention()).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("E qual canal devo avisar sobre level ups? Nulo não é um canal válido!").queue();
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
                            gc.setMsgBoasVindas(message.getMessage().getContentRaw().replace(gc.getPrefix() + "definir msgbv ", "").replace("\"", ""));
                            message.getChannel().sendMessage("Agora irei dizer __**" + gc.getMsgBoasVindas() + "**__ para usuários que entrarem no servidor!").queue();
                        } else {
                            message.getChannel().sendMessage("A mensagem deve estar entre aspas (\")").queue();
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Você não me disse que mensagem devo dizer quando alguém entrar!").queue();
                    }
                    break;
                case "msgadeus":
                    try {
                        if (cmd[2].contains("\"")) {
                            gc.setMsgAdeus(message.getMessage().getContentRaw().replace(gc.getPrefix() + "definir msgadeus ", "").replace("\"", ""));
                            message.getChannel().sendMessage("Agora irei dizer __**" + gc.getMsgAdeus() + "**__ para membros que deixarem o servidor!").queue();
                        } else {
                            message.getChannel().sendMessage("A mensagem deve estar entre aspas (\")").queue();
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Você não me disse que mensagem devo dizer quando alguém sair!").queue();
                    }
                    break;
                case "cargolvl":
                    try {
                        int lvl = Integer.parseInt(cmd[2]);
                        if (message.getMessage().getMentionedRoles().size() != 0) {
                            Map<String, Object> cargos = gc.getCargoslvl();
                            cargos.put(cmd[2], message.getMessage().getMentionedRoles().get(0).getId());
                            gc.setCargoslvl(new JSONObject(cargos));
                        } else {
                            message.getChannel().sendMessage("Por favor, mencione um cargo né!").queue();
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Erro ao adicionar cargo.").queue();
                    }
                    break;
                case "cargonovo":
                    try {
                        int lvl = Integer.parseInt(cmd[2]);
                        if (message.getMessage().getMentionedRoles().size() != 0) {
                            Map<String, Object> cargos = gc.getCargoNew();
                            cargos.put(cmd[2], message.getMessage().getMentionedRoles().get(0).getId());
                            gc.setCargoNew(new JSONObject(cargos));
                        } else {
                            message.getChannel().sendMessage("Por favor, mencione um cargo né!").queue();
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Erro ao adicionar cargo.").queue();
                    }
                    break;
                default:
                    message.getChannel().sendMessage("Não conheço esse comando, certeza que digitou corretamente?").queue();
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            message.getChannel().sendMessage("Você precisa me dizer o quê devo definir").queue();
        }
    }

    public static void addWarn(MessageReceivedEvent message, String reason, Map<String, Member> m) {
        m.get(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId()).addWarn(reason);
        message.getChannel().sendMessage("O usuário " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " teve um alerta registrado pelo seguinte motivo: `" + reason + "`").queue();
    }

    public static void takeWarn(MessageReceivedEvent message, Map<String, Member> m) {
        m.get(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId()).removeWarn(Integer.parseInt(message.getMessage().getContentRaw().split(" ")[2]));
        message.getChannel().sendMessage("Foi retirado o alerta " + message.getMessage().getContentRaw().split(" ")[2] + " um alerta do usuário " + message.getMessage().getMentionedUsers().get(0).getAsMention()).queue();
    }
}
