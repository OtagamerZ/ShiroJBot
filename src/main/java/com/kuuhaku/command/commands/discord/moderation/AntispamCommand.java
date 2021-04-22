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
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

@Command(
		name = "semspam",
		aliases = {"nospam", "antispam"},
		usage = "req_spam-type-amount",
		category = Category.MODERATION
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY, Permission.MANAGE_ROLES})
public class AntispamCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length > 0 && Helper.equalsAny(args[0], "soft", "hard")) {
			switch (args[0].toLowerCase(Locale.ROOT)) {
				case "soft" -> {
					if (!gc.isHardAntispam()) {
						channel.sendMessage("❌ | O modo **SOFT** já está ligado").queue();
						return;
					}

					gc.setHardAntispam(false);
					channel.sendMessage("✅ | Modo de anti-spam trocado para **SOFT**").queue();
				}
				case "hard" -> {
					if (gc.isHardAntispam()) {
						channel.sendMessage("❌ | O modo **HARD** já está ligado").queue();
						return;
					}

					gc.setHardAntispam(true);
					channel.sendMessage("✅ | Modo de anti-spam trocado para **HARD**").queue();
				}
			}

			GuildDAO.updateGuildSettings(gc);
			return;
		} else if (args.length > 0 && StringUtils.isNumeric(args[0])) {
			try {
				int amount = Integer.parseInt(args[0]);
				if (amount < 5) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-message-threshold")).queue();
					return;
				}

				gc.setNoSpamAmount(amount);
			} catch (NumberFormatException e) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-amount")).queue();
			}
		}

		if (!gc.getNoSpamChannels().contains(channel.getId())) {
			gc.addNoSpamChannel(message.getTextChannel().getId());
			channel.sendMessage("✅ | Modo antispam ativado neste canal. (" + gc.getNoSpamAmount() + " mensagens)").queue();
		} else {
			gc.removeNoSpamChannel(message.getTextChannel().getId());
			channel.sendMessage("✅ | Modo antispam neste canal desativado.").queue();
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
