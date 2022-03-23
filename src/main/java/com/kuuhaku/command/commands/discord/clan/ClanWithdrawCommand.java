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

package com.kuuhaku.command.commands.discord.clan;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "sacar",
		aliases = {"withdraw", "sac", "with"},
		usage = "req_qtd",
		category = Category.CLAN
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class ClanWithdrawCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (c.isLocked(author.getId(), ClanPermission.WITHDRAW)) {
			channel.sendMessage("❌ | Você não tem permissão para sacar do cofre do clã.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa especificar uma quantia de CR para serem sacados.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		int value = Integer.parseInt(args[0]);

		if (c.getVault() < value) {
			channel.sendMessage("❌ | O cofre do clã não possui CR suficientes.").queue();
			return;
		}

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Tem certeza que deseja sacar " + Helper.separate(value) + " CR do cofre do clã?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());
							s.delete().queue(null, Helper::doNothing);

							Clan finalC = ClanDAO.getUserClan(author.getId());
							assert finalC != null;
							if (finalC.getVault() < value) {
								channel.sendMessage("❌ | O cofre do clã não possui CR suficientes.").queue();
								return;
							}

							acc.addCredit(value, this.getClass());
							finalC.withdraw(value, author);

							ClanDAO.saveClan(finalC);
							AccountDAO.saveAccount(acc);

							s.delete().flatMap(d -> channel.sendMessage("✅ | Valor sacado com sucesso.")).queue();
						}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}
}
