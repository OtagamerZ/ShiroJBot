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
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.apache.commons.lang3.StringUtils;

public class AntiraidCommand extends Command {

	public AntiraidCommand() {
		super("semraid", new String[]{"noraid", "antiraid"}, "Expulsa automaticamente novos membros que possuirem contas muito recentes (< 10 min).", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		guildConfig gc = SQLite.getGuildById(guild.getId());

		if (gc.isAntiRaid())
			gc.setAntiRaid(false);
		else gc.setAntiRaid(true);

		SQLite.updateGuildChannels(gc);

		if (!gc.isAntiRaid())
			channel.sendMessage("Modo anti-raid está desligado").queue();
		else
			channel.sendMessage("Modo anti-raid está ligado, expulsarei novos membros que tiverem uma conta com tempo menor que 10 minutos.").queue();
	}
}
