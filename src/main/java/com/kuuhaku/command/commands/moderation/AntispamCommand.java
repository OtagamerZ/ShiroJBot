/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class AntispamCommand extends Command {

	public AntispamCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AntispamCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AntispamCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AntispamCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length > 0 && (args[0].equalsIgnoreCase("soft") || args[0].equalsIgnoreCase("hard"))) {
			switch (args[0].toLowerCase()) {
				case "soft":
					if (!gc.isHardAntispam()) {
						channel.sendMessage("O modo **SOFT** já está ligado").queue();
						return;
					}
					gc.setHardAntispam(false);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage("Modo de anti-spam trocado para **SOFT**").queue();
					return;
				case "hard":
					if (gc.isHardAntispam()) {
						channel.sendMessage("O modo **HARD** já está ligado").queue();
						return;
					}
					gc.setHardAntispam(true);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessage("Modo de anti-spam trocado para **HARD**").queue();
					return;
			}
			return;
		}

		for (String s : args) {
			if (StringUtils.isNumeric(s)) {
				if (Integer.parseInt(s) < 5) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_antispam-invalid-arguments")).queue();
					return;
				}
				gc.setNoSpamAmount(Integer.parseInt(s));
			}
		}

		if (gc.getNoSpamChannels().contains(channel.getId()))
			gc.removeNoSpamChannel(message.getTextChannel());
		else gc.addNoSpamChannel(message.getTextChannel());

		GuildDAO.updateGuildSettings(gc);

		if (!gc.getNoSpamChannels().contains(channel.getId()))
			channel.sendMessage("Modo antispam neste canal está desligado").queue();
		else
			channel.sendMessage("Modo antispam neste canal está ligado (" + gc.getNoSpamAmount() + " mensagens)").queue();
	}
}
