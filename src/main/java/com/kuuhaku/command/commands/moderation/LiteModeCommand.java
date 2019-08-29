/*
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
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class LiteModeCommand extends Command {

	public LiteModeCommand() {
		super("litemode", new String[]{"lite"}, "Ativa ou desativa o modo lite do relay", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		guildConfig gc = SQLite.getGuildById(guild.getId());

		if (gc.isLiteMode()) {
			gc.setLiteMode(false);
			channel.sendMessage("Modo lite está desligado").queue();
		} else {
			gc.setLiteMode(true);
			channel.sendMessage("Modo lite está ligado, as mensagens globais estarão em um padrão mais leve.").queue();
		}

		SQLite.updateGuildSettings(gc);
	}
}
