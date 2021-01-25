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

package com.kuuhaku.command.commands.discord.clan;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ClanWithdrawCommand extends Command {

	public ClanWithdrawCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ClanWithdrawCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ClanWithdrawCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ClanWithdrawCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (!c.hasPermission(author.getId(), ClanPermission.WITHDRAW)) {
			channel.sendMessage("❌ | Você não tem permissão para sacar do cofre do clã.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa especificar uma quantia de créditos para serem sacados.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_amount-not-valid")).queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		int amount = Integer.parseInt(args[0]);

		if (c.getVault() < amount) {
			channel.sendMessage("❌ | O cofre do clã não tem créditos suficientes.").queue();
			return;
		}

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Tem certeza que deseja sacar " + Helper.separate(amount) + " créditos do cofre do clã?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (!ShiroInfo.getHashes().remove(hash)) return;
							Main.getInfo().getConfirmationPending().invalidate(author.getId());

							acc.addCredit(amount, this.getClass());
							c.withdraw(amount, author);

							ClanDAO.saveClan(c);
							AccountDAO.saveAccount(acc);

							s.delete().flatMap(d -> channel.sendMessage("✅ | Valor sacado com sucesso.")).queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}
}
