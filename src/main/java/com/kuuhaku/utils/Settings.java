package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Settings {

    public static void embedConfig(Message message) {
        try {
            guildConfig gc = GuildDAO.getGuildById(message.getGuild().getId());
            String prefix = Helper.getOr(gc.getPrefix(), "s!");

            String canalBV = Helper.getOr(gc.getCanalBV(), "Não definido.");
            if (!canalBV.equals("Não definido.")) canalBV = "<#" + canalBV + ">";
            String msgBV = Helper.getOr(gc.getMsgBoasVindas(), "Não definido.");
            if (!msgBV.equals("Não definido.")) msgBV = "`" + msgBV + "`";

            String canalAdeus = Helper.getOr(gc.getCanalAdeus(), "Não definido.");
            if (!canalAdeus.equals("Não definido.")) canalAdeus = "<#" + canalAdeus + ">";
            String msgAdeus = Helper.getOr(gc.getMsgAdeus(), "Não definido.");
            if (!msgAdeus.equals("Não definido.")) msgAdeus = "`" + msgAdeus + "`";

            String canalSUG = Helper.getOr(gc.getCanalSUG(), "Não definido.");
            if (!canalSUG.equals("Não definido.")) canalSUG = "<#" + canalSUG + ">";

            int pollTime = gc.getPollTime();

            String canalLvlUpNotif = Helper.getOr(gc.getCanalLvl(), "Não definido.");
            if (!canalLvlUpNotif.equals("Não definido.")) canalLvlUpNotif = "<#" + canalLvlUpNotif + ">";

            StringBuilder cargosLvl = new StringBuilder();
            if (gc.getCargoslvl() != null) {
                List<Integer> lvls = gc.getCargoslvl().keySet().stream().map(Integer::parseInt).sorted().collect(Collectors.toList());
                for (int i : lvls) {
                    try {
                        Map<String, Object> cargos = gc.getCargoslvl();
                        Role role = message.getGuild().getRoleById((String) cargos.get(String.valueOf(i)));
                        cargosLvl.append(i).append(" - ").append(Objects.requireNonNull(role).getAsMention()).append("\n");
                    } catch (NullPointerException e) {
                        Map<String, Object> cn = gc.getCargoslvl();
                        cn.remove(String.valueOf(i));
                        gc.setCargosLvl(new JSONObject(cn));
                        GuildDAO.updateGuildSettings(gc);
                    }
                }
            }

            String canalRelay = Helper.getOr(gc.getCanalRelay(), "Não definido.");
            if (!canalRelay.equals("Não definido.")) canalRelay = "<#" + canalRelay + ">";

            String cargoWarnID = Helper.getOr(gc.getCargoWarn(), "Não definido.");
            int warnTime = gc.getWarnTime();
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
            eb.addBlankField(true);
            eb.addBlankField(true);
            eb.addField("\uD83D\uDCD6 » Canal de Sugestões", canalSUG, true);
            eb.addField("\u23F2 » Tempo de enquetes", String.valueOf(pollTime), true);
            try {
                if (TagDAO.getTagById(Objects.requireNonNull(message.getGuild().getOwner()).getUser().getId()).isPartner()) {
                    eb.addField("\uD83D\uDCD6 » Canal Relay", canalRelay, true);
                }
            } catch (NoResultException ignore) {
            }

            if (!cargoWarnID.equals("Não definido.")) {
                try {
                    eb.addField("\uD83D\uDCD1 » Cargo de punição", Main.getInfo().getRoleByID(cargoWarnID).getAsMention(), true);
                } catch (NullPointerException e) {
                    gc.setCargoWarn(null);
                    GuildDAO.updateGuildSettings(gc);
                }
            } else {
                eb.addField("\uD83D\uDCD1 » Cargo de punição", cargoWarnID, true);
            }

            eb.addField("\u23F2 » Tempo de punição", String.valueOf(warnTime), true);

            //if(!cargoNewID.equals("Não definido.")) { eb.addField("\uD83D\uDCD1 » Cargo automático", com.kuuhaku.Main.getInfo().getRoleByID(cargoNewID).getAsMention(), false); }
            //else { eb.addField("\uD83D\uDCD1 » Cargos automáticos", cargoNewID, true); }

            eb.addField("\uD83D\uDCD6 » Canal de notificação de level up", canalLvlUpNotif, true);
            eb.addField("\uD83D\uDCD1 » Cargos de nível", cargosLvl.toString().isEmpty() ? "Nenhum" : cargosLvl.toString(), true);


            eb.setFooter("Para obter ajuda sobre como configurar o seu servidor, use `" + GuildDAO.getGuildById(message.getGuild().getId()).getPrefix() + "settings ajuda`", null);

            message.getTextChannel().sendMessage(eb.build()).queue();
        } catch (Exception err) {
            message.getChannel().sendMessage(":x: | Ocorreu um erro durante o processo, os meus developers já foram notificados.").queue();
            Helper.logger(Settings.class).error(err + " | " + err.getStackTrace()[0]);
        }
    }

    public static void updatePrefix(String[] args, Message message, guildConfig gc) {
        if (args.length < 2) {
            message.getTextChannel().sendMessage("O prefixo atual deste servidor é `" + GuildDAO.getGuildById(message.getGuild().getId()).getPrefix() + "`.").queue();
            return;
        }

        String newPrefix = args[1].trim();
        if (newPrefix.length() > 5) {
            message.getTextChannel().sendMessage(":x: | O prefixo `" + newPrefix + "` contem mais de 5 carateres, não pode.").queue();
            return;
        }

        gc.setPrefix(newPrefix);
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O prefixo deste servidor foi trocado para `" + newPrefix + "` com sucesso.").queue();
    }

    public static void updateCanalBV(String[] args, Message message, guildConfig gc) {
        String antigoCanalBVID = GuildDAO.getGuildById(message.getGuild().getId()).getCanalBV();

        if (args.length < 2) {
            if (antigoCanalBVID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de boas-vindas atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de boas-vindas atual do servidor é <#" + antigoCanalBVID + ">.").queue();
            }
            return;
        }
        if (message.getMentionedChannels().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setCanalBV(null);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | O canal de boas-vindas do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalBV = message.getMentionedChannels().get(0);

        gc.setCanalBV(newCanalBV.getId());
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O canal de boas-vindas do servidor foi trocado para " + newCanalBV.getAsMention() + " com sucesso.").queue();
    }

    public static void updateMsgBV(String[] args, Message message, guildConfig gc) {
        String antigaMsgBV = GuildDAO.getGuildById(message.getGuild().getId()).getMsgBoasVindas();

        if (args.length < 2) {
            message.getTextChannel().sendMessage("A mensagem de boas-vindas atual do servidor é `" + antigaMsgBV + "`.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setMsgBoasVindas("Seja bem-vindo(a) ao %guild%, %user%!");
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi resetado com sucesso.").queue();
            return;
        }

        String newMsgBv = String.join(" ", args).replace(args[0], "").trim();

        gc.setMsgBoasVindas(newMsgBv);
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi trocado para " + newMsgBv + " com sucesso.").queue();
    }

    public static void updateCanalAdeus(String[] args, Message message, guildConfig gc) {
        String antigoCanalAdeusID = GuildDAO.getGuildById(message.getGuild().getId()).getCanalAdeus();

        if (args.length < 2) {
            if (antigoCanalAdeusID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de adeus atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de adeus atual do servidor é <#" + antigoCanalAdeusID + ">.").queue();
            }
            return;
        }
        if (message.getMentionedChannels().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setCanalAdeus(null);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalAdeus = message.getMentionedChannels().get(0);

        gc.setCanalAdeus(newCanalAdeus.getId());
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi trocado para " + newCanalAdeus.getAsMention() + " com sucesso.").queue();
    }

    public static void updateMsgAdeus(String[] args, Message message, guildConfig gc) {
        String antigaMsgAdeus = GuildDAO.getGuildById(message.getGuild().getId()).getMsgAdeus();

        if (args.length < 2) {
            message.getTextChannel().sendMessage("A mensagem de adeus atual do servidor é `" + antigaMsgAdeus + "`.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setMsgAdeus("Ahhh...%user% saiu do servidor!");
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | A mensagem de adeus do servidor foi resetada com sucesso.").queue();
            return;
        }

        String newMsgAdeus = String.join(" ", args).replace(args[0], "").trim();

        gc.setMsgAdeus(newMsgAdeus);
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | A mensagem de adeus do servidor foi trocada para " + newMsgAdeus + " com sucesso.").queue();
    }

    public static void updateCanalSUG(String[] args, Message message, guildConfig gc) {
        String antigoCanalSUGID = GuildDAO.getGuildById(message.getGuild().getId()).getCanalSUG();

        if (args.length < 2) {
            if (antigoCanalSUGID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de sugestões atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de sugestões atual do servidor é <#" + antigoCanalSUGID + ">.").queue();
            }
            return;
        }
        if (message.getMentionedChannels().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setCanalSUG(null);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | O canal de sugestões do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalSUG = message.getMentionedChannels().get(0);

        gc.setCanalSUG(newCanalSUG.getId());
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O canal de sugestões do servidor foi trocado para " + newCanalSUG.getAsMention() + " com sucesso.").queue();
    }

    public static void updatePollTime(String[] args, Message message, guildConfig gc) {
        int antigoPollTime = GuildDAO.getGuildById(message.getGuild().getId()).getPollTime();

        if (args.length < 2) {
            message.getTextChannel().sendMessage("O tempo de enquetes atual do servidor é " + antigoPollTime + " segundos.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setPollTime(60);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | O tempo de enquetes do servidor foi resetado para 60 segundos com sucesso.").queue();
            return;
        } else if (!StringUtils.isNumeric(args[1])) {
            message.getTextChannel().sendMessage(":x: | O tempo inserido é inválido, ele deve ser um valor inteiro.").queue();
            return;
        }

        int newPollTime = Integer.parseInt(args[1]);

        gc.setPollTime(newPollTime);
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O tempo de enquetes do servidor foi trocado para " + newPollTime + " segundos com sucesso.").queue();
    }

    public static void updateCargoWarn(String[] args, Message message, guildConfig gc) {
        String antigoCargoWarn = GuildDAO.getGuildById(message.getGuild().getId()).getCargoWarn();

        if (args.length < 2) {
            if (antigoCargoWarn.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O cargo de warns atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O cargo de warns atual do servidor é `" + antigoCargoWarn + "`.").queue();
            }
            return;
        }
        if (message.getMentionedRoles().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 cargo.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setCargoWarn(null);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | O cargo de warns do servidor foi resetado com sucesso.").queue();
            return;
        }

        Role newRoleWarns = message.getMentionedRoles().get(0);

        gc.setCargoWarn(newRoleWarns.getId());
        GuildDAO.updateGuildSettings(gc);
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
        boolean LevelUpNotif = GuildDAO.getGuildById(message.getGuild().getId()).isLvlNotif();

        if (args.length < 2) {
            if (LevelUpNotif) {
                message.getTextChannel().sendMessage("As mensagens quando alguém sobe de nível estão ativas.").queue();
            } else {
                message.getTextChannel().sendMessage("As mensagens quando alguém sobe de nível não estão ativas.").queue();
            }
            return;
        }
        if (message.getMentionedChannels().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue();
            return;
        }
        if (args[1].equals("ativar") || args[1].equals("sim")) {
            gc.setLvlNotif(true);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | As mensagens quando alguém sobe de nível foram ativadas com sucesso.").queue();
        } else if (args[1].equals("desativar") || args[1].equals("nao") || args[1].equals("não")) {
            gc.setLvlNotif(false);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | As mensagens quando alguém sobe de nível foram desativadas com sucesso.").queue();
        } else {
            message.getTextChannel().sendMessage(":x: | \"" + args[1] + "\" não é uma opção válida, por favor escolha \"ativar\" ou então \"desativar\".").queue();
        }
    }

    public static void updateCanalLevelUp(String[] args, Message message, guildConfig gc) {
        String antigoCanalLvlUpID = GuildDAO.getGuildById(message.getGuild().getId()).getCanalLvl();

        if (args.length < 2) {
            if (antigoCanalLvlUpID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal de level up atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal de level up atual do servidor é <#" + antigoCanalLvlUpID + ">.").queue();
            }
            return;
        }
        if (message.getMentionedChannels().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setCanalLvl(null);
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | O canal de level up do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalLvlUp = message.getMentionedChannels().get(0);

        gc.setCanalLvl(newCanalLvlUp.getId());
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O canal de level up do servidor foi trocado para " + newCanalLvlUp.getAsMention() + " com sucesso.").queue();
    }

    public static void updateCanalRelay(String[] args, Message message, guildConfig gc) {
        String antigoCanalRelayID = GuildDAO.getGuildById(message.getGuild().getId()).getCanalRelay();

        if (args.length < 2) {
            if (antigoCanalRelayID.equals("Não definido.")) {
                message.getTextChannel().sendMessage("O canal relay atual do servidor ainda não foi definido.").queue();
            } else {
                message.getTextChannel().sendMessage("O canal relay atual do servidor é <#" + antigoCanalRelayID + ">.").queue();
            }
            return;
        }
        if (message.getMentionedChannels().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 canal.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setCanalRelay(null);
            message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi resetado com sucesso.").queue();
            return;
        }

        TextChannel newCanalRelay = message.getMentionedChannels().get(0);

        gc.setCanalRelay(newCanalRelay.getId());
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi trocado para " + newCanalRelay.getAsMention() + " com sucesso.").queue();
    }

    public static void updateCargoLvl(String[] args, Message message, guildConfig gc) {
        Map<String, Object> antigoCargoLvl = GuildDAO.getGuildById(message.getGuild().getId()).getCargoslvl();
        List<Integer> lvls = antigoCargoLvl.keySet().stream().map(Integer::parseInt).sorted().collect(Collectors.toList());
        StringBuilder cargosLvl = new StringBuilder();
        for (int i : lvls) {
            cargosLvl.append(i).append(" - ").append(Objects.requireNonNull(message.getGuild().getRoleById((String) antigoCargoLvl.get(String.valueOf(i)))).getAsMention()).append("\n");
        }

        if (args.length < 3) {
            if (antigoCargoLvl.size() == 0) {
                message.getTextChannel().sendMessage("Nenhum cargo por level foi definido ainda.").queue();
            } else {
                message.getTextChannel().sendMessage("Os cargos por level definidos são:```" + cargosLvl.toString() + "```").queue();
            }
            return;
        } else if (!StringUtils.isNumeric(args[2])) {
            message.getTextChannel().sendMessage(":x: | O terceiro argumento deve ser uma valor inteiro").queue();
            return;
        } else if (message.getMentionedRoles().size() > 1) {
            message.getTextChannel().sendMessage(":x: | Você só pode mencionar 1 cargo por vez.").queue();
            return;
        } else if (args[1].equals("reset") || args[1].equals("resetar")) {
            try {
                Map<String, Object> cl = gc.getCargoslvl();
                cl.remove(args[2]);
                GuildDAO.updateGuildSettings(gc);
                message.getTextChannel().sendMessage("✅ | O cargo dado no level " + args[2] + " do servidor foi resetado com sucesso.").queue();
                return;
            } catch (Exception e) {
                message.getTextChannel().sendMessage(":x: | Este nível ainda não possui nenhum cargo.").queue();
                return;
            }
        }

        Role newRoleLevel = message.getMentionedRoles().get(0);

        Map<String, Object> cl = gc.getCargoslvl();
        cl.put(args[2], newRoleLevel);
        GuildDAO.updateGuildSettings(gc);
        message.getTextChannel().sendMessage("✅ | O cargo dado no level " + args[2] + " do servidor foi trocado para " + newRoleLevel.getAsMention() + " com sucesso.").queue();
    }

    public static void updateModules(String[] args, Message message, guildConfig gc) {
        List<Category> antigoModulo = GuildDAO.getGuildById(message.getGuild().getId()).getDisabledModules();

        if (args[1].equals("reset") || args[1].equals("resetar")) {
            gc.setDisabledModules(new ArrayList<>());
            GuildDAO.updateGuildSettings(gc);
            message.getTextChannel().sendMessage("✅ | Todos os módulos habilitados novamente.").queue();
            return;
        } else if (args.length < 3) {
            if (antigoModulo.size() == 0) {
                message.getTextChannel().sendMessage("Nenhum módulo desligado.").queue();
            } else {
                message.getTextChannel().sendMessage("Os módulos desligados são:```\n" + antigoModulo.stream().map(c -> c.getName() + "\n").collect(Collectors.joining()) + "```").queue();
            }
            return;
        } else if (Helper.containsAny(args[1].toLowerCase(), "moderação", "dev", "parceiros")) {
            message.getTextChannel().sendMessage(":x: | É impossível desativar o módulo " + args[1].toLowerCase() + ".").queue();
        }

        try {
        	if (Helper.containsAny(args[2].toLowerCase(), "ligado", "enabled", "on")) {
        		if (!antigoModulo.contains(Category.getByName(args[1]))) {
					message.getTextChannel().sendMessage(":x: | Módulo já ativado.").queue();
				} else {
        			antigoModulo.remove(Category.getByName(args[1]));
				}
			} else if (Helper.containsAny(args[2].toLowerCase(), "desligado", "disabled", "off")) {
				if (antigoModulo.contains(Category.getByName(args[1]))) {
					message.getTextChannel().sendMessage(":x: | Módulo já desativado.").queue();
				} else {
					antigoModulo.add(Category.getByName(args[1]));
				}
			} else {
				message.getTextChannel().sendMessage(":x: | O terceiro argumento deve ser ligado ou desligado").queue();
				return;
			}

        	gc.setDisabledModules(antigoModulo);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | Módulo " + args[1] + " " + (antigoModulo.contains(Category.getByName(args[1])) ? "desabilitado" : "habilitado") + " com sucesso.").queue();
		} catch (RuntimeException e) {
			message.getTextChannel().sendMessage(":x: | Módulo inválido.").queue();
		}
    }
}
