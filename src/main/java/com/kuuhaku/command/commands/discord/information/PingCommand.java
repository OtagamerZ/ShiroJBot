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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;

public class PingCommand extends Command {

	public PingCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PingCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PingCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PingCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		int fp = (int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
		channel.sendMessage(":ping_pong: Pong! ")
				.flatMap(m -> m.editMessage(m.getContentRaw() + """
						 %s ms!
						:floppy_disk: %s MB!
						:telephone: %s 
						""".formatted(
						Main.getInfo().getAPI().getGatewayPing(),
						fp,
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_listeners"), Main.getInfo().getAPI().getEventManager().getRegisteredListeners().size())
				)))
				.queue();

		if (author.getId().equals(ShiroInfo.getNiiChan())) {
			User u = Main.getInfo().getUserByID(author.getId());
			Account acc = AccountDAO.getAccount(author.getId());
			if (u != null && acc.isReceivingNotifs()) u.openPrivateChannel().queue(c -> {
				double share = ExceedDAO.getMemberShare(u.getId());
				long total = Math.round(ExceedDAO.getExceed(ExceedEnum.IMANITY).getExp() / 1000f);
				long prize = Math.round(total * share);
				try {
					c.sendMessage("""
							O seu Exceed foi campeão neste mês, parabéns!
							Todos da %s ganharão experiência em dobro durante 1 semana além de isenção de taxas.
							Adicionalmente, por ter sido responsável por %s%% da pontuação de seu Exceed, você receberá %s créditos como parte do prêmio (Total: %s).
							""".formatted("Imanity", Helper.roundToString(share, 2), prize, total)).queue(null, Helper::doNothing);
				} catch (Exception ignore) {
				}
			});
		}
	}
}
