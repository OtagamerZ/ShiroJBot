/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "semraid",
		aliases = {"noraid", "antiraid"},
		usage = "req_threshold",
		category = Category.MODERATION
)
@Requires({Permission.KICK_MEMBERS})
public class AntiraidCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length > 0 && StringUtils.isNumeric(args[0])) {
			try {
				int threshold = Integer.parseInt(args[0]);
				if (threshold < 5) {
					channel.sendMessage("❌ | O limiar deve ser maior que 5.").queue();
					return;
				}

				gc.setAntiRaidLimit(threshold);
				gc.setAntiRaid(true);
				channel.sendMessage("✅ | Modo antiraid ativado, expulsarei caso entrem " + gc.getAntiRaidLimit() + " novos membros em um intervalo de 5 segundos.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
			}
		} else {
			gc.toggleAntiRaid();

			if (gc.isAntiRaid())
				channel.sendMessage("✅ | Modo antiraid ativado, expulsarei caso entrem " + gc.getAntiRaidLimit() + " novos membros em um intervalo de 5 segundos.").queue();
			else
				channel.sendMessage("✅ | Modo antiraid desativado.").queue();
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
