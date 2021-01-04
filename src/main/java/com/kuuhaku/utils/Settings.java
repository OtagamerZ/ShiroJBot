/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.persistence.NoResultException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Settings {

	public static void embedConfig(Message message) {
		try {
			GuildConfig gc = GuildDAO.getGuildById(message.getGuild().getId());
			String prefix = Helper.getOr(gc.getPrefix(), "s!");

			//CANAIS
			String canalBV = Helper.getOr(gc.getCanalBV(), "Não definido.");
			if (!canalBV.equals("Não definido.")) canalBV = "<#" + canalBV + ">";

			String canalAdeus = Helper.getOr(gc.getCanalAdeus(), "Não definido.");
			if (!canalAdeus.equals("Não definido.")) canalAdeus = "<#" + canalAdeus + ">";

			String canalSUG = Helper.getOr(gc.getCanalSUG(), "Não definido.");
			if (!canalSUG.equals("Não definido.")) canalSUG = "<#" + canalSUG + ">";

			String canalLvlUpNotif = Helper.getOr(gc.getCanalLvl(), "Não definido.");
			if (!canalLvlUpNotif.equals("Não definido.")) canalLvlUpNotif = "<#" + canalLvlUpNotif + ">";

			String canalRelay = Helper.getOr(gc.getCanalRelay(), "Não definido.");
			if (!canalRelay.equals("Não definido.")) canalRelay = "<#" + canalRelay + ">";

			String canalKawaipon = Helper.getOr(gc.getCanalKawaipon(), "Não definido.");
			if (!canalKawaipon.equals("Não definido.")) canalKawaipon = "<#" + canalKawaipon + ">";

			String canalDrop = Helper.getOr(gc.getCanalDrop(), "Não definido.");
			if (!canalDrop.equals("Não definido.")) canalDrop = "<#" + canalDrop + ">";

			String canalAvisos = Helper.getOr(gc.getCanalAvisos(), "Não definido.");
			if (!canalAvisos.equals("Não definido.")) canalAvisos = "<#" + canalAvisos + ">";

			String canalGeral = Helper.getOr(gc.getCanalGeral(), "Não definido.");
			if (!canalGeral.equals("Não definido.")) canalGeral = "<#" + canalGeral + ">";

			//MENSAGENS
			String msgBV = Helper.getOr(gc.getMsgBoasVindas(), "Não definido.");
			if (!msgBV.equals("Não definido.")) msgBV = "```" + msgBV + "```";

			String msgAdeus = Helper.getOr(gc.getMsgAdeus(), "Não definido.");
			if (!msgAdeus.equals("Não definido.")) msgAdeus = "```" + msgAdeus + "```";

			String generalTopic = Helper.getOr(gc.getGeneralTopic(), "Não definido.");
			if (!generalTopic.equals("Não definido.")) generalTopic = "```" + generalTopic + "```";

			//TEMPOS
			int pollTime = gc.getPollTime();
			int warnTime = gc.getWarnTime();

			//CARGOS
			String cargoMuteID = Helper.getOr(gc.getCargoMute(), "Não definido.");
			//String cargoVipID = Helper.getOr(gc.getCargoVip(), "Não definido.");
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

			EmbedBuilder eb;

			if (message.getGuild().getIconUrl() != null) {
				eb = new EmbedBuilder();
				eb.setThumbnail(message.getGuild().getIconUrl());
				eb.setColor(Helper.colorThief(message.getGuild().getIconUrl()));
			} else eb = new ColorlessEmbedBuilder();

			eb.setTitle("⚙ | Configurações do servidor");
			eb.addField("\uD83D\uDD17 » Prefixo: __" + prefix + "__", Helper.VOID, false);

			eb.addField("\uD83D\uDCD6 » Canal de Boas-vindas", canalBV, false);
			eb.addField("\uD83D\uDCDD » Mensagem de Boas-vindas", msgBV, false);
			eb.addField(Helper.VOID + "\n\uD83D\uDCD6 » Canal de Adeus", canalAdeus, false);
			eb.addField("\uD83D\uDCDD » Mensagem de Adeus", msgAdeus, false);
			eb.addField(Helper.VOID + "\n\uD83D\uDCD6 » Canal geral", canalAvisos, true);
			eb.addField("\uD83D\uDCDD » Tópico do canal geral", generalTopic, false);

			eb.addBlankField(false);

			eb.addField("⏲ » Tempo de enquetes", String.valueOf(pollTime), true);
			eb.addField("⏲ » Tempo de punição", String.valueOf(warnTime), true);

			eb.addBlankField(false);

			eb.addField("\uD83D\uDCD6 » Canal de sugestões", canalSUG, true);
			eb.addField("\uD83D\uDCD6 » Canal de notificação nível", canalLvlUpNotif, true);
			eb.addField("\uD83D\uDCD6 » Canal de cartas Kawaipon", canalKawaipon, true);
			eb.addField("\uD83D\uDCD6 » Canal de drops de créditos", canalDrop, true);
			eb.addField("\uD83D\uDCD6 » Canal de avisos", canalAvisos, true);
			try {
				if (TagDAO.getTagById(Objects.requireNonNull(message.getGuild().getOwner()).getUser().getId()).isBeta()) {
					eb.addField("\uD83D\uDCD6 » Canal Relay", canalRelay, true);
				}
			} catch (NoResultException ignore) {
			}

			eb.addBlankField(false);

			eb.addField("\uD83D\uDCD1 » Cargos de nível", cargosLvl.toString().isEmpty() ? "Nenhum" : cargosLvl.toString(), true);
			if (!cargoMuteID.equals("Não definido.")) {
				try {
					eb.addField("\uD83D\uDCD1 » Cargo de punição", Main.getInfo().getRoleByID(cargoMuteID).getAsMention(), true);
				} catch (NullPointerException e) {
					gc.setCargoMute(null);
					GuildDAO.updateGuildSettings(gc);
				}
			} else {
				eb.addField("\uD83D\uDCD1 » Cargo de punição", cargoMuteID, true);
			}

			/*if (!cargoVipID.equals("Não definido.")) {
				try {
					eb.addField("\uD83D\uDCD1 » Cargo VIP", Main.getInfo().getRoleByID(cargoVipID).getAsMention(), true);
				} catch (NullPointerException e) {
					gc.setCargoVip(null);
					GuildDAO.updateGuildSettings(gc);
				}
			} else {
				eb.addField("\uD83D\uDCD1 » Cargo VIP", cargoVipID, true);
			}*/

			eb.setFooter("Para obter ajuda sobre como configurar o seu servidor, use `" + gc.getPrefix() + "settings ajuda`", null);

			message.getTextChannel().sendMessage(eb.build()).queue();
		} catch (Exception err) {
			message.getChannel().sendMessage("❌ | Ocorreu um erro durante o processo, os meus developers já foram notificados.").queue();
			Helper.logger(Settings.class).error(err + " | " + err.getStackTrace()[0]);
		}
	}

	public static void updatePrefix(String[] args, Message message, GuildConfig gc) {
		if (args.length < 2) {
			message.getTextChannel().sendMessage("O prefixo atual deste servidor é `" + gc.getPrefix() + "`.").queue();
			return;
		}

		String newPrefix = args[1].trim();
		if (newPrefix.length() > 5) {
			message.getTextChannel().sendMessage("❌ | O prefixo `" + newPrefix + "` contem mais de 5 carateres, não pode.").queue();
			return;
		} else if (newPrefix.length() < 2) {
			message.getTextChannel().sendMessage("❌ | O prefixo `" + newPrefix + "` contem menos de 2 carateres, também não pode.").queue();
			return;
		}

		gc.setPrefix(newPrefix);
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O prefixo deste servidor foi trocado para `" + newPrefix + "` com sucesso.").queue();
	}

	public static void updateCanalBV(String[] args, Message message, GuildConfig gc) {
		String antigoCanalBVID = gc.getCanalBV();

		if (args.length < 2) {
			if (antigoCanalBVID.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O canal de boas-vindas atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O canal de boas-vindas atual do servidor é <#" + antigoCanalBVID + ">.").queue();
			}
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCanalBV(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O canal de boas-vindas do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedChannels().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel newCanalBV = message.getMentionedChannels().get(0);

		gc.setCanalBV(newCanalBV.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O canal de boas-vindas do servidor foi trocado para " + newCanalBV.getAsMention() + " com sucesso.").queue();
	}

	public static void updateMsgBV(String[] args, Message message, GuildConfig gc) {
		String antigaMsgBV = gc.getMsgBoasVindas();

		if (args.length < 2) {
			message.getTextChannel().sendMessage("A mensagem de boas-vindas atual do servidor é `" + antigaMsgBV + "`.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setMsgBoasVindas("Seja bem-vindo(a) ao %guild%, %user%!");
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi resetado com sucesso.").queue();
			return;
		}

		String newMsgBv = String.join(" ", args)
				.replace(args[0], "")
				.replace("\\n", "\n")
				.trim();

		gc.setMsgBoasVindas(newMsgBv);
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | A mensagem de boas-vindas do servidor foi trocado para " + newMsgBv + " com sucesso.").queue();
	}

	public static void updateCanalAdeus(String[] args, Message message, GuildConfig gc) {
		String antigoCanalAdeusID = gc.getCanalAdeus();

		if (args.length < 2) {
			if (antigoCanalAdeusID.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O canal de adeus atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O canal de adeus atual do servidor é <#" + antigoCanalAdeusID + ">.").queue();
			}
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCanalAdeus(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedChannels().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel newCanalAdeus = message.getMentionedChannels().get(0);

		gc.setCanalAdeus(newCanalAdeus.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O canal de adeus do servidor foi trocado para " + newCanalAdeus.getAsMention() + " com sucesso.").queue();
	}

	public static void updateMsgAdeus(String[] args, Message message, GuildConfig gc) {
		String antigaMsgAdeus = gc.getMsgAdeus();

		if (args.length < 2) {
			message.getTextChannel().sendMessage("A mensagem de adeus atual do servidor é `" + antigaMsgAdeus + "`.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setMsgAdeus("Ahhh...%user% saiu do servidor!");
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | A mensagem de adeus do servidor foi resetada com sucesso.").queue();
			return;
		}

		String newMsgAdeus = String.join(" ", args)
				.replace(args[0], "")
				.replace("\\n", "\n")
				.trim();

		gc.setMsgAdeus(newMsgAdeus);
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | A mensagem de adeus do servidor foi trocada para " + newMsgAdeus + " com sucesso.").queue();
	}

	public static void updateGeneralTopic(String[] args, Message message, GuildConfig gc) {
		String antigoGeneralTopic = gc.getGeneralTopic();

		if (args.length < 2) {
			message.getTextChannel().sendMessage("O tópico do canal geral atual do servidor é `" + antigoGeneralTopic + "`.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setGeneralTopic("Contagem de membros em %count% e subindo!");
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O tópico do canal geral foi resetada com sucesso.").queue();
			return;
		}

		String newGeneralTopic = String.join(" ", args)
				.replace(args[0], "")
				.replace("\\n", "\n")
				.trim();

		gc.setGeneralTopic(newGeneralTopic);
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O tópico do canal geral foi trocado para " + newGeneralTopic + " com sucesso.").queue();
	}

	public static void updateCanalSUG(String[] args, Message message, GuildConfig gc) {
		String antigoCanalSUGID = gc.getCanalSUG();

		if (args.length < 2) {
			if (antigoCanalSUGID.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O canal de sugestões atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O canal de sugestões atual do servidor é <#" + antigoCanalSUGID + ">.").queue();
			}
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCanalSUG("");
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O canal de sugestões do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedChannels().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel newCanalSUG = message.getMentionedChannels().get(0);

		gc.setCanalSUG(newCanalSUG.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O canal de sugestões do servidor foi trocado para " + newCanalSUG.getAsMention() + " com sucesso.").queue();
	}

	public static void updateWarnTime(String[] args, Message message, GuildConfig gc) {
		int antigoWarnTime = gc.getWarnTime();

		if (args.length < 2) {
			message.getTextChannel().sendMessage("O tempo de punições atual do servidor é " + antigoWarnTime + " minutos.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setWarnTime(60);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O tempo de punições do servidor foi resetado para 60 minutos com sucesso.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			message.getTextChannel().sendMessage("❌ | O tempo inserido é inválido, ele deve ser um valor inteiro.").queue();
			return;
		}

		int newWarnTime = Integer.parseInt(args[1]);

		gc.setWarnTime(newWarnTime);
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O tempo de punições do servidor foi trocado para " + newWarnTime + " minutos com sucesso.").queue();
	}

	public static void updatePollTime(String[] args, Message message, GuildConfig gc) {
		int antigoPollTime = gc.getPollTime();

		if (args.length < 2) {
			message.getTextChannel().sendMessage("O tempo de enquetes atual do servidor é " + antigoPollTime + " segundos.").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setPollTime(60);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O tempo de enquetes do servidor foi resetado para 60 segundos com sucesso.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			message.getTextChannel().sendMessage("❌ | O tempo inserido é inválido, ele deve ser um valor inteiro.").queue();
			return;
		}

		int newPollTime = Integer.parseInt(args[1]);

		gc.setPollTime(newPollTime);
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O tempo de enquetes do servidor foi trocado para " + newPollTime + " segundos com sucesso.").queue();
	}

	public static void updateCargoMute(String[] args, Message message, GuildConfig gc) {
		String antigoCargoMute = gc.getCargoMute();

		if (args.length < 2) {
			if (antigoCargoMute.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O cargo de punição atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O cargo de punição atual do servidor é `" + antigoCargoMute + "`.").queue();
			}
			return;
		}
		if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCargoMute(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O cargo de punição do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedRoles().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-role")).queue();
			return;
		}

		Role newCargoMute = message.getMentionedRoles().get(0);

		gc.setCargoMute(newCargoMute.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O cargo de punição do servidor foi trocado para " + newCargoMute.getAsMention() + " com sucesso.").queue();
	}

	public static void updateCargoVip(String[] args, Message message, GuildConfig gc) {
		String antigoCargoVip = gc.getCargoVip();

		if (args.length < 2) {
			if (antigoCargoVip.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O cargo VIP atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O cargo VIP atual do servidor é `" + antigoCargoVip + "`.").queue();
			}
			return;
		}
		if (message.getMentionedRoles().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-role")).queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCargoVip(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O cargo VIP do servidor foi resetado com sucesso.").queue();
			return;
		}

		Role newRoleVip = message.getMentionedRoles().get(0);

		gc.setCargoVip(newRoleVip.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O cargo VIP do servidor foi trocado para " + newRoleVip.getAsMention() + " com sucesso.").queue();
	}

	public static void updateLevelNotif(String[] args, Message message, GuildConfig gc) {
		boolean LevelUpNotif = gc.isLvlNotif();

		if (args.length < 2) {
			if (LevelUpNotif) {
				message.getTextChannel().sendMessage("As mensagens quando alguém sobe de nível estão ativas.").queue();
			} else {
				message.getTextChannel().sendMessage("As mensagens quando alguém sobe de nível não estão ativas.").queue();
			}
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
			message.getTextChannel().sendMessage("❌ | \"" + args[1] + "\" não é uma opção válida, por favor escolha \"ativar\" ou então \"desativar\".").queue();
		}
	}

	public static void updateCanalLevelUp(String[] args, Message message, GuildConfig gc) {
		String antigoCanalLvlUpID = gc.getCanalLvl();

		if (args.length < 2) {
			if (antigoCanalLvlUpID.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O canal de level up atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O canal de level up atual do servidor é <#" + antigoCanalLvlUpID + ">.").queue();
			}
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCanalLvl(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O canal de level up do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedChannels().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel newCanalLvlUp = message.getMentionedChannels().get(0);

		gc.setCanalLvl(newCanalLvlUp.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O canal de level up do servidor foi trocado para " + newCanalLvlUp.getAsMention() + " com sucesso.").queue();
	}

	public static void updateCanalRelay(String[] args, Message message, GuildConfig gc) {
		String antigoCanalRelayID = gc.getCanalRelay();

		if (args.length < 2) {
			if (antigoCanalRelayID.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O canal relay atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O canal relay atual do servidor é <#" + antigoCanalRelayID + ">.").queue();
			}
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCanalRelay(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedChannels().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel newCanalRelay = message.getMentionedChannels().get(0);

		gc.setCanalRelay(newCanalRelay.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O canal relay do servidor foi trocado para " + newCanalRelay.getAsMention() + " com sucesso.").queue();
	}

	public static void updateCanalAvisos(String[] args, Message message, GuildConfig gc) {
		String antigoCanalAvisosID = gc.getCanalAvisos();

		if (args.length < 2) {
			if (antigoCanalAvisosID.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O canal de avisos atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O canal de avisos atual do servidor é <#" + antigoCanalAvisosID + ">.").queue();
			}
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCanalAvisos(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O canal de avisos do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedChannels().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel newCanalAvisos = message.getMentionedChannels().get(0);

		gc.setCanalAvisos(newCanalAvisos.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O canal de avisos do servidor foi trocado para " + newCanalAvisos.getAsMention() + " com sucesso.").queue();
	}

	public static void updateCanalGeral(String[] args, Message message, GuildConfig gc) {
		String antigoCanalGeral = gc.getCanalAvisos();

		if (args.length < 2) {
			if (antigoCanalGeral.equals("Não definido.")) {
				message.getTextChannel().sendMessage("O canal geral atual do servidor ainda não foi definido.").queue();
			} else {
				message.getTextChannel().sendMessage("O canal geral atual do servidor é <#" + antigoCanalGeral + ">.").queue();
			}
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			gc.setCanalGeral(null);
			GuildDAO.updateGuildSettings(gc);
			message.getTextChannel().sendMessage("✅ | O canal geral do servidor foi resetado com sucesso.").queue();
			return;
		} else if (message.getMentionedChannels().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-channel")).queue();
			return;
		}

		TextChannel newCanalGeral = message.getMentionedChannels().get(0);

		gc.setCanalGeral(newCanalGeral.getId());
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O canal geral do servidor foi trocado para " + newCanalGeral.getAsMention() + " com sucesso.").queue();
	}

	public static void updateCargoLvl(String[] args, Message message, GuildConfig gc) {
		Map<String, Object> antigoCargoLvl = gc.getCargoslvl();
		List<Integer> lvls = antigoCargoLvl.keySet().stream().map(Integer::parseInt).sorted().collect(Collectors.toList());
		StringBuilder cargosLvl = new StringBuilder();
		for (int i : lvls) {
			cargosLvl.append(i).append(" - ").append(Objects.requireNonNull(message.getGuild().getRoleById((String) antigoCargoLvl.get(String.valueOf(i)))).getName()).append("\n");
		}

		if (args.length < 3) {
			if (antigoCargoLvl.size() == 0) {
				message.getTextChannel().sendMessage("Nenhum cargo por level foi definido ainda.").queue();
			} else {
				message.getTextChannel().sendMessage("Os cargos por level definidos são:```" + cargosLvl.toString() + "```").queue();
			}
			return;
		} else if (!StringUtils.isNumeric(args[2])) {
			message.getTextChannel().sendMessage("❌ | O terceiro argumento deve ser uma valor inteiro").queue();
			return;
		} else if (args[1].equals("reset") || args[1].equals("resetar")) {
			try {
				Map<String, Object> cl = gc.getCargoslvl();
				cl.remove(args[2]);
				gc.setCargosLvl(new JSONObject(cl));
				GuildDAO.updateGuildSettings(gc);
				message.getTextChannel().sendMessage("✅ | O cargo dado no level " + args[2] + " do servidor foi resetado com sucesso.").queue();
				return;
			} catch (Exception e) {
				message.getTextChannel().sendMessage("❌ | Este nível ainda não possui nenhum cargo.").queue();
				return;
			}
		} else if (message.getMentionedRoles().size() < 1) {
			message.getTextChannel().sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-role")).queue();
			return;
		}

		Role newRole = message.getMentionedRoles().get(0);

		antigoCargoLvl.put(args[2], newRole.getId());
		gc.setCargosLvl(new JSONObject(antigoCargoLvl));
		GuildDAO.updateGuildSettings(gc);
		message.getTextChannel().sendMessage("✅ | O cargo " + newRole.getAsMention() + " foi atribuido ao level " + args[2] + " com sucesso.").queue();
	}

	public static void updateModules(String[] args, Message message, GuildConfig gc) {
		List<String> antigoModulo = gc.getDisabledModules();

		if (Helper.equalsAny(args[1], "list", "lista")) {
			if (antigoModulo.size() == 0) {
				message.getTextChannel().sendMessage("Nenhum módulo desligado.").queue();
			} else {
				message.getTextChannel().sendMessage("Os módulos desligados são:```" + String.join("\n", antigoModulo) + "```").queue();
			}
			return;
		} else if (Helper.equalsAny(args[1].toLowerCase(), "moderação", "dev", "parceiros")) {
			message.getTextChannel().sendMessage("❌ | É impossível desativar o módulo " + args[1].toLowerCase() + ".").queue();
			return;
		}

		Category c = Category.getByName(args[1]);

		if (c == null) {
			message.getTextChannel().sendMessage("❌ | Esse módulo não existe.").queue();
			return;
		} else if (Helper.equalsAny(args[2].toLowerCase(), "ligado", "enabled", "on")) {
			if (!antigoModulo.contains(c.name())) {
				message.getTextChannel().sendMessage("❌ | Esse módulo já está ativado.").queue();
				return;
			}

			gc.removeDisabledModule(c);
			message.getTextChannel().sendMessage("✅ | Módulo " + c.getName() + " habilitado com sucesso.").queue();
		} else if (Helper.equalsAny(args[2].toLowerCase(), "desligado", "disabled", "off")) {
			if (antigoModulo.contains(c.name())) {
				message.getTextChannel().sendMessage("❌ | Esse módulo já está desativado.").queue();
				return;
			}

			gc.addDisabledModule(c);
			message.getTextChannel().sendMessage("✅ | Módulo " + c.getName() + " desabilitado com sucesso.").queue();
		} else {
			message.getTextChannel().sendMessage("❌ | O terceiro argumento deve ser ligado ou desligado").queue();
			return;
		}

		GuildDAO.updateGuildSettings(gc);
	}

	public static void settingsHelp(Message message, GuildConfig gc) {
		String prefix = Helper.getOr(gc.getPrefix(), "s!");

		EmbedBuilder eb;
		if (message.getGuild().getIconUrl() != null) eb = new EmbedBuilder();
		else eb = new ColorlessEmbedBuilder();

		if (message.getGuild().getIconUrl() != null) eb.setThumbnail(message.getGuild().getIconUrl());
		eb.setTitle("⚙ | Painel de ajuda");
		eb.setDescription("Utilize os comandos a baixo para estabelecer suas configurações.");
		eb.addField(prefix + "settings prefix", "Altera o prefixo da Shiro no seu servidor.", false);
		eb.addField(prefix + "settings categoria", "Ativa ou desativa uma categoria de comandos.", false);

		eb.addField(prefix + "settings canalbv", "Define o canal onde a Shiro ira mandar as mensagens de boas-vindas. Para remover esta configuração, use `" + prefix + "settings canalbv reset`.", false);
		eb.addField(prefix + "settings mensagembv", "Defina uma mensagem de boas-vindas em seu servidor.", false);
		eb.addField(prefix + "settings canaladeus", "Define o canal onde a Shiro ira mandar as mensagens de saída. Para remover esta configuração, use `" + prefix + "settings canaladeus reset`.", false);
		eb.addField(prefix + "settings mensagemadeus", "Defina uma mensagem de saída em seu servidor.", false);
		eb.addField(prefix + "settings canalgeral", "Define o canal geral do seu servidor. Para remover esta configuração, use `" + prefix + "settings canalgeral reset`.", false);
		eb.addField(prefix + "settings mensagemadeus", "Defina o tópico do canal geral do seu servidor.", false);

		eb.addField(prefix + "settings canalsug", "Define o canal de sugestões em seu servidor. Para remover esta configuração, use `" + prefix + "settings canalsug reset`.", false);
		eb.addField(prefix + "settings canallevelup", "Define o canal de level up em seu servidor. Para remover esta configuração, use `" + prefix + "settings canallevelup reset`.", false);
		eb.addField(prefix + "settings canalrelay", "Define o canal de relay em seu servidor. Para remover esta configuração, use `" + prefix + "settings canalrelay reset`.", false);
		eb.addField(prefix + "settings canalavisos", "Define o canal de avisos em seu servidor. Para remover esta configuração, use `" + prefix + "settings canalavisos reset`.", false);

		eb.addField(prefix + "settings tempowarn", "Define o tempo de punições em seu servidor.", false);
		eb.addField(prefix + "settings tempopoll", "Define o tempo de enquetes em seu servidor.", false);

		eb.addField(prefix + "settings cargomute", "Define o cargo de punição em seu servidor. Para remover esta configuração, use `" + prefix + "settings cargomute reset`.", false);
		//eb.addField(prefix + "settings rolevip", "Define o cargo VIP em seu servidor.", false);
		eb.addField(prefix + "settings cargolevel", "Define os cargos por level em seu servidor. Para remover esta configuração, use `" + prefix + "settings cargolevel reset LEVEL`.", false);
		eb.addField(prefix + "settings levelnotif", "Habilita as notificações de nível.\n\nParâmetros:\n", false);

		eb.addField("%guild%", "Para dizer o nome do server.", false);
		eb.addField("%user%", "Para dizer o nome do usuário.", false);
		eb.addField("%count%", "(Apenas tópico) Para dizer a contagem de membros.", false);

		message.getTextChannel().sendMessage(eb.build()).queue();
	}
}
