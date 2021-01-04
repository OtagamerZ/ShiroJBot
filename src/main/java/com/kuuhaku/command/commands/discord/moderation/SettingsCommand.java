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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Settings;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class SettingsCommand extends Command {

	public SettingsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SettingsCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SettingsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SettingsCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length == 0) {
			Settings.embedConfig(message);
			return;
		} else if (args.length == 1 && !(args[0].toLowerCase().equals("ajuda") || args[0].toLowerCase().equals("help"))) {
			channel.sendMessage("❌ | Nenhum argumento informado para a configuração `" + args[0] + "`.").queue();
			return;
		}

		final String msg;

		if (args.length == 1) msg = "";
		else msg = String.join(" ", args).replace(args[0], "").replace(args[1], "").trim();

		/*case "rvip":
			case "rolevip":
				Settings.updateCargoVip(args, message, gc);
				break;*/
		switch (args[0].toLowerCase()) {
			case "prefix", "prefixo" -> {
				if (msg.length() > 5) {
					channel.sendMessage("❌ | Prefixo muito longo (Max. 5)").queue();
					return;
				}
				Settings.updatePrefix(args, message, gc);
			}
			case "cbv", "canalbv" -> Settings.updateCanalBV(args, message, gc);
			case "mensagembemvindo", "mensagembv", "msgbv" -> {
				if (msg.length() > 2000) {
					channel.sendMessage("❌ | Mensagem muito longa (Max. 2000 caractéres)").queue();
					return;
				}
				Settings.updateMsgBV(args, message, gc);
			}
			case "cadeus", "canaladeus" -> Settings.updateCanalAdeus(args, message, gc);
			case "mensagemadeus", "mensagema", "msgadeus" -> {
				if (msg.length() > 2000) {
					channel.sendMessage("❌ | Mensagem muito longa (Max. 2000 caractéres)").queue();
					return;
				}
				Settings.updateMsgAdeus(args, message, gc);
			}
			case "cg", "canalgeral" -> Settings.updateCanalGeral(args, message, gc);
			case "topico", "tp", "tpgeral" -> {
				if (msg.length() > 500) {
					channel.sendMessage("❌ | Tópico muito longo (Max. 500 caractéres)").queue();
					return;
				}
				Settings.updateGeneralTopic(args, message, gc);
			}
			case "tmute", "tempomute", "tmu" -> Settings.updateWarnTime(args, message, gc);
			case "tpoll", "tempopoll" -> Settings.updatePollTime(args, message, gc);
			case "csug", "canalsug" -> Settings.updateCanalSUG(args, message, gc);
			case "cmute", "cargomute", "rmute", "rolemute" -> Settings.updateCargoMute(args, message, gc);
			case "ln", "levelnotif" -> Settings.updateLevelNotif(args, message, gc);
			case "canallevelup", "canallvlup", "clvlup" -> Settings.updateCanalLevelUp(args, message, gc);
			case "canalrelay", "canalrly", "crelay" -> Settings.updateCanalRelay(args, message, gc);
			case "canalavisos", "canalav", "cavisos" -> Settings.updateCanalAvisos(args, message, gc);
			case "clvl", "cargolevel", "rlvl", "rolelevel" -> Settings.updateCargoLvl(args, message, gc);
			case "mod", "module", "categoria", "cat:" -> Settings.updateModules(args, message, gc);
			case "help", "ajuda" -> Settings.settingsHelp(message, gc);
			default -> Settings.embedConfig(message);
		}
	}
}
