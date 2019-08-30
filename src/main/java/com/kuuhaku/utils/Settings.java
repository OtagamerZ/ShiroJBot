package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Settings {

	public static void embedConfig(Message message) throws IOException {
		guildConfig gc = SQLite.getGuildById(message.getGuild().getId());
		String prefix = SQLite.getGuildPrefix(message.getGuild().getId());

		String canalBV = SQLite.getGuildCanalBV(message.getGuild().getId());
		if (!canalBV.equals("Não definido.")) canalBV = "<#" + canalBV + ">";
		String msgBV = SQLite.getGuildMsgBV(message.getGuild().getId());
		if (!msgBV.equals("Não definido.")) msgBV = "`" + msgBV + "`";

		String canalAdeus = SQLite.getGuildCanalAdeus(message.getGuild().getId());
		if (!canalAdeus.equals("Não definido.")) canalAdeus = "<#" + canalAdeus + ">";
		String msgAdeus = SQLite.getGuildMsgAdeus(message.getGuild().getId());
		if (!msgAdeus.equals("Não definido.")) msgAdeus = "`" + msgAdeus + "`";

		String canalSUG = SQLite.getGuildCanalSUG(message.getGuild().getId());
		if (!canalSUG.equals("Não definido.")) canalSUG = "<#" + canalSUG + ">";

		int pollTime = SQLite.getGuildPollTime(message.getGuild().getId());

		String canalLvlUpNotif = SQLite.getGuildCanalLvlUp(message.getGuild().getId());
		if (!canalLvlUpNotif.equals("Não definido.")) canalLvlUpNotif = "<#" + canalLvlUpNotif + ">";

		StringBuilder cargosLvl = new StringBuilder();
		if (SQLite.getGuildCargosLvl(message.getGuild().getId()) != null) {
			List<Integer> lvls = gc.getCargoslvl().keySet().stream().map(Integer::parseInt).sorted().collect(Collectors.toList());
			for (int i : lvls) {
				try {
					Map<String, Object> cargos = SQLite.getGuildCargosLvl(message.getGuild().getId());
					Role role = message.getGuild().getRoleById((String) cargos.get(String.valueOf(i)));
					cargosLvl.append(i).append(" - ").append(Objects.requireNonNull(role).getAsMention()).append("\n");
				} catch (NullPointerException e) {
					SQLite.updateGuildCargosLvl(String.valueOf(i), null, gc);
				}
			}
		}

		String canalRelay = SQLite.getGuildCanalRelay(message.getGuild().getId());
		if (!canalRelay.equals("Não definido.")) canalRelay = "<#" + canalRelay + ">";

		String cargoWarnID = SQLite.getGuildCargoWarn(message.getGuild().getId());
		int warnTime = SQLite.getGuildWarnTime(message.getGuild().getId());
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
		if (MySQL.getTagById(Objects.requireNonNull(message.getGuild().getOwner()).getUser().getId()).isPartner()) {
			eb.addField("\uD83D\uDCD6 » Canal Relay", canalRelay, true);
		}

		if (!cargoWarnID.equals("Não definido.")) {
			eb.addField("\uD83D\uDCD1 » Cargo de punição", Main.getInfo().getRoleByID(cargoWarnID).getAsMention(), true);
		} else {
			eb.addField("\uD83D\uDCD1 » Cargo de punição", cargoWarnID, true);
		}

		eb.addField("\u23F2 » Tempo de punição", String.valueOf(warnTime), true);

		//if(!cargoNewID.equals("Não definido.")) { eb.addField("\uD83D\uDCD1 » Cargo automático", com.kuuhaku.Main.getInfo().getRoleByID(cargoNewID).getAsMention(), false); }
		//else { eb.addField("\uD83D\uDCD1 » Cargos automáticos", cargoNewID, true); }

		eb.addField("\uD83D\uDCD6 » Canal de notificação de level up", canalLvlUpNotif, true);
		eb.addField("\uD83D\uDCD1 » Cargos de nível", cargosLvl.toString().isEmpty() ? "Nenhum" : cargosLvl.toString(), true);


		eb.setFooter("Para obter ajuda sobre como configurar o seu servidor, use " + SQLite.getGuildPrefix(message.getGuild().getId()) + "settings ajuda", null);

		message.getTextChannel().sendMessage(eb.build()).queue();
	}

	public static void updatePrefix(String[] args, Message message, guildConfig gc) {
		if (args.length < 2) {
			message.getTextChannel().sendMessage("O prefixo atual deste servidor é `" + SQLite.getGuildPrefix(message.getGuild().getId()) + "`.").queue();
			return;
		}

		String newPrefix = args[1].trim();
		if (newPrefix.length() > 5) {
			message.getTextChannel().sendMessage(":x: | O prefixo `" + newPrefix + "` contem mais de 5 carateres, não pode.").queue();
			return;
		}

		SQLite.updateGuildPrefix(newPrefix, gc);
		message.getTextChannel().sendMessage("✅ | O prefixo deste servidor foi trocado para `" + newPrefix + "` com sucesso.").queue();
	}

	public static void updateCanalBV(String[] args, Message message, guildConfig gc) {
		String antigoCanalBVID = SQLite.getGuildCanalBV(message.getGuild().getId());

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
			SQLite.updateGuildCanalBV(null, gc);
			message.getTextChannel().sendMessage("✅ | O canal de boas-vindas do servidor foi resetado com sucesso.").queue();
			return;
		}

		TextChannel newCanalBV = message.getMentionedChannels().get(0);

		SQLite.updateGuildCanalBV(newCanalBV.getId(), gc);
		message.getTextChannel().sendMessage("✅ | O canal de boas-vindas do servidor foi trocado para " + newCanalBV.getAsMention() + " com sucesso.").queue();
	}

	public static void updateMsgBV(String[] args, Message message, guildConfig gc) {
		String antigaMsgBV = SQLite.getGuildMsgBV(message.getGuild().getId());

		if (args.length < 2) {
			message.getTextChannel().sendMessage("A mensagem de boas-vindas atual do servidor é `" + antigaMsgBV + "`.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			SQLite.updateGuildMsgBV("Seja bem-vindo(a) ao %guild%, %user%!", gc);
			message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi resetado com sucesso.").queue();
			return;
		}

		String newMsgBv = String.join(" ", args).replace(args[0], "").trim();

		SQLite.updateGuildMsgBV(newMsgBv, gc);
		message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi trocado para " + newMsgBv + " com sucesso.").queue();
	}

	public static void updateCanalAdeus(String[] args, Message message, guildConfig gc) {
		String antigoCanalAdeusID = SQLite.getGuildCanalAdeus(message.getGuild().getId());

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

		if (args.length < 2) {
			message.getTextChannel().sendMessage("A mensagem de adeus atual do servidor é `" + antigaMsgAdeus + "`.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			SQLite.updateGuildMsgAdeus("Ahhh...%user% saiu do servidor!", gc);
			message.getTextChannel().sendMessage("✅ | A mensagem de adeus do servidor foi resetada com sucesso.").queue();
			return;
		}

		String newMsgAdeus = String.join(" ", args).replace(args[0], "").trim();

		SQLite.updateGuildMsgAdeus(newMsgAdeus, gc);
		message.getTextChannel().sendMessage("✅ | A mensagem de adeus do servidor foi trocada para " + newMsgAdeus + " com sucesso.").queue();
	}

	public static void updateCanalSUG(String[] args, Message message, guildConfig gc) {
		String antigoCanalSUGID = SQLite.getGuildCanalSUG(message.getGuild().getId());

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
			SQLite.updateGuildCanalSUG(null, gc);
			message.getTextChannel().sendMessage("✅ | O canal de sugestões do servidor foi resetado com sucesso.").queue();
			return;
		}

		TextChannel newCanalSUG = message.getMentionedChannels().get(0);

		SQLite.updateGuildCanalSUG(newCanalSUG.getId(), gc);
		message.getTextChannel().sendMessage("✅ | O canal de sugestões do servidor foi trocado para " + newCanalSUG.getAsMention() + " com sucesso.").queue();
	}

	public static void updatePollTime(String[] args, Message message, guildConfig gc) {
		int antigoPollTime = SQLite.getGuildPollTime(message.getGuild().getId());

		if (args.length < 2) {
			message.getTextChannel().sendMessage("O tempo de enquetes atual do servidor é " + antigoPollTime + " segundos.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			SQLite.updateGuildPollTime(60, gc);
			message.getTextChannel().sendMessage("✅ | O tempo de enquetes do servidor foi resetado para 60 segundos com sucesso.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			message.getTextChannel().sendMessage(":x: | O tempo inserido é inválido, ele deve ser um valor inteiro.").queue();
			return;
		}

		int newPollTime = Integer.parseInt(args[1]);

		SQLite.updateGuildPollTime(newPollTime, gc);
		message.getTextChannel().sendMessage("✅ | O tempo de enquetes do servidor foi trocado para " + newPollTime + " segundos com sucesso.").queue();
	}

	public static void updateCargoWarn(String[] args, Message message, guildConfig gc) {
		String antigoCargoWarn = SQLite.getGuildCargoWarn(message.getGuild().getId());

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
			SQLite.updateGuildLvlUpNotif(true, gc);
			message.getTextChannel().sendMessage("✅ | As mensagens quando alguém sobe de nível foram ativadas com sucesso.").queue();
		} else if (args[1].equals("desativar") || args[1].equals("nao") || args[1].equals("não")) {
			SQLite.updateGuildLvlUpNotif(false, gc);
			message.getTextChannel().sendMessage("✅ | As mensagens quando alguém sobe de nível foram desativadas com sucesso.").queue();
		} else {
			message.getTextChannel().sendMessage(":x: | \"" + args[1] + "\" não é uma opção válida, por favor escolha \"ativar\" ou então \"desativar\".").queue();
		}
	}

	public static void updateCanalLevelUp(String[] args, Message message, guildConfig gc) {
		String antigoCanalLvlUpID = SQLite.getGuildCanalLvlUp(message.getGuild().getId());

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
			SQLite.updateGuildCanalRelay(null, gc);
			message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi resetado com sucesso.").queue();
			return;
		}

		TextChannel newCanalRelay = message.getMentionedChannels().get(0);

		SQLite.updateGuildCanalRelay(newCanalRelay.getId(), gc);
		message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi trocado para " + newCanalRelay.getAsMention() + " com sucesso.").queue();
	}

	public static void updateCargoLvl(String[] args, Message message, guildConfig gc) {
		Map<String, Object> antigoCargoLvl = SQLite.getGuildCargosLvl(message.getGuild().getId());
		List<Integer> lvls = SQLite.getGuildCargosLvl(message.getGuild().getId()).keySet().stream().map(Integer::parseInt).sorted().collect(Collectors.toList());
		StringBuilder cargosLvl = new StringBuilder();
		for (int i : lvls) {
			Map<String, Object> cargos = SQLite.getGuildCargosLvl(message.getGuild().getId());
			cargosLvl.append(i).append(" - ").append(Objects.requireNonNull(message.getGuild().getRoleById((String) cargos.get(String.valueOf(i)))).getAsMention()).append("\n");
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
				SQLite.updateGuildCargosLvl(args[2], null, gc);
				message.getTextChannel().sendMessage("✅ | O cargo dado no level " + args[2] + " do servidor foi resetado com sucesso.").queue();
				return;
			} catch (Exception e) {
				message.getTextChannel().sendMessage(":x: | Este nível ainda não possui nenhum cargo.").queue();
				return;
			}
		}

		Role newRoleLevel = message.getMentionedRoles().get(0);

		SQLite.updateGuildCargosLvl(args[2], newRoleLevel, gc);
		message.getTextChannel().sendMessage("✅ | O cargo dado no level " + args[2] + " do servidor foi trocado para " + newRoleLevel.getAsMention() + " com sucesso.").queue();
	}
}
