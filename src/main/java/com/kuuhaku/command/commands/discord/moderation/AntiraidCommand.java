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
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "semraid",
		aliases = {"noraid", "antiraid"},
		usage = "req_minutes",
		category = Category.MODERATION
)
@Requires({Permission.KICK_MEMBERS})
public class AntiraidCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length > 0 && StringUtils.isNumeric(args[0])) {
			try {
				int time = Integer.parseInt(args[0]);

				if (time < 10) {
					channel.sendMessage("❌ | O tempo precisa ser maior ou igual a 10 minutos.").queue();
					return;
				}

				gc.setAntiRaidTime(time);
				gc.setAntiRaid(true);
				channel.sendMessage("Modo anti-raid está ligado, expulsarei novos membros que tiverem uma conta com tempo menor que " + gc.getAntiRaidTime() + " minutos.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_amount-not-valid")).queue();
			}
		} else {
			if (gc.isAntiRaid()) {
				gc.setAntiRaid(false);
				channel.sendMessage("Modo anti-raid está desligado").queue();
			} else {
				gc.setAntiRaid(true);
				channel.sendMessage("Modo anti-raid está ligado, expulsarei novos membros que tiverem uma conta com tempo menor que " + gc.getAntiRaidTime() + " minutos.").queue();
			}
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
