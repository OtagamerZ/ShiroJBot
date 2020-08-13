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

		switch (args[0].toLowerCase()) {
			case "prefix":
			case "prefixo":
				if (msg.length() > 5) {
					channel.sendMessage("❌ | Prefixo muito longo (Max. 5)").queue();
					return;
				}
				Settings.updatePrefix(args, message, gc);
				break;
			case "cbv":
			case "canalbv":
				Settings.updateCanalBV(args, message, gc);
				break;
			case "mensagembemvindo":
			case "mensagembv":
			case "msgbv":
				if (msg.length() > 2000) {
					channel.sendMessage("❌ | Mensagem muito longa (Max. 2000 caractéres)").queue();
					return;
				}
				Settings.updateMsgBV(args, message, gc);
				break;
			case "cadeus":
			case "canaladeus":
				Settings.updateCanalAdeus(args, message, gc);
				break;
			case "mensagemadeus":
			case "mensagema":
			case "msgadeus":
				if (msg.length() > 2000) {
					channel.sendMessage("❌ | Mensagem muito longa (Max. 2000 caractéres)").queue();
					return;
				}
				Settings.updateMsgAdeus(args, message, gc);
				break;
			case "twarn":
			case "tempowarn":
			case "tpun":
				Settings.updateWarnTime(args, message, gc);
				break;
			case "tpoll":
			case "tempopoll":
				Settings.updatePollTime(args, message, gc);
				break;
			case "csug":
			case "canalsug":
				Settings.updateCanalSUG(args, message, gc);
				break;
			case "rwarn":
			case "rolewarn":
				Settings.updateCargoWarn(args, message, gc);
				break;
			/*case "rvip":
			case "rolevip":
				Settings.updateCargoVip(args, message, gc);
				break;*/
			case "ln":
			case "levelnotif":
				Settings.updateLevelNotif(args, message, gc);
				break;
			case "canallevelup":
			case "canallvlup":
			case "clvlup":
				Settings.updateCanalLevelUp(args, message, gc);
				break;
			case "canalrelay":
			case "canalrly":
			case "crelay":
				Settings.updateCanalRelay(args, message, gc);
				break;
			case "canalavisos":
			case "canalav":
			case "cavisos":
				Settings.updateCanalAvisos(args, message, gc);
				break;
			case "clvl":
			case "cargolevel":
			case "cargolvl":
				Settings.updateCargoLvl(args, message, gc);
				break;
			case "mod":
			case "module":
			case "categoria":
			case "cat:":
				Settings.updateModules(args, message, gc);
				break;
			case "help":
			case "ajuda":
				Settings.settingsHelp(message, gc);
				break;
			default:
				Settings.embedConfig(message);
		}
	}
}
