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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "parametros",
		aliases = {"settings", "configs", "definicoes"},
		usage = "req_parameter",
		category = Category.MODERATION
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class SettingsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length == 0) {
			Settings.embedConfig(message);
			return;
		} else if (args.length == 1 && !(args[0].equalsIgnoreCase("ajuda") || args[0].equalsIgnoreCase("help"))) {
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
				Settings.updatePrefix(args, argsAsText, message, gc);
			}
			case "cbv", "canalbv" -> Settings.updateCanalBV(args, argsAsText, message, gc);
			case "mensagembemvindo", "mensagembv", "msgbv" -> {
				if (msg.length() > 2000) {
					channel.sendMessage("❌ | Mensagem muito longa (Max. 2000 caractéres)").queue();
					return;
				}
				Settings.updateMsgBV(args, argsAsText, message, gc);
			}
			case "cadeus", "canaladeus" -> Settings.updateCanalAdeus(args, argsAsText, message, gc);
			case "mensagemadeus", "mensagema", "msgadeus" -> {
				if (msg.length() > 2000) {
					channel.sendMessage("❌ | Mensagem muito longa (Max. 2000 caractéres)").queue();
					return;
				}
				Settings.updateMsgAdeus(args, argsAsText, message, gc);
			}
			case "cg", "canalgeral" -> Settings.updateCanalGeral(args, argsAsText, message, gc);
			case "topico", "tp", "tpgeral" -> {
				if (msg.length() > 500) {
					channel.sendMessage("❌ | Tópico muito longo (Max. 500 caractéres)").queue();
					return;
				}
				Settings.updateGeneralTopic(args, argsAsText, message, gc);
			}
			case "tmute", "tempomute", "tmu" -> Settings.updateWarnTime(args, argsAsText, message, gc);
			case "tpoll", "tempopoll" -> Settings.updatePollTime(args, argsAsText, message, gc);
			case "csug", "canalsug" -> Settings.updateCanalSUG(args, argsAsText, message, gc);
			case "cmute", "cargomute", "rmute", "rolemute" -> Settings.updateCargoMute(args, argsAsText, message, gc);
			case "ln", "levelnotif" -> Settings.updateLevelNotif(args, argsAsText, message, gc);
			case "canallevelup", "canallvlup", "clvlup" -> Settings.updateCanalLevelUp(args, argsAsText, message, gc);
			case "canalrelay", "canalrly", "crelay" -> Settings.updateCanalRelay(args, argsAsText, message, gc);
			case "canalavisos", "canalav", "cavisos" -> Settings.updateCanalAvisos(args, argsAsText, message, gc);
			case "clvl", "cargolevel", "rlvl", "rolelevel" -> Settings.updateCargoLvl(args, argsAsText, message, gc);
			case "mod", "module", "categoria", "cat:" -> Settings.updateModules(args, argsAsText, message, gc);
			case "help", "ajuda" -> Settings.settingsHelp(message, gc);
			default -> Settings.embedConfig(message);
		}
	}
}
