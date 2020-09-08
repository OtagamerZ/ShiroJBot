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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class TransferCommand extends Command {

	public TransferCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public TransferCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public TransferCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public TransferCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_transfer-no-amount")).queue();
			return;
		} else if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-transfer-to-yourself")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_amount-not-valid")).queue();
			return;
		}


		Account from = AccountDAO.getAccount(author.getId());
		Account to = AccountDAO.getAccount(message.getMentionedUsers().get(0).getId());

		boolean victorious = ExceedDAO.hasExceed(author.getId()) && Main.getInfo().getWinner().equals(ExceedDAO.getExceed(author.getId()));
		int rawAmount = Integer.parseInt(args[0]);
		int tax = victorious ? 0 : (int) Math.floor(rawAmount * 0.025);
		int liquidAmount = rawAmount - tax;

		if (from.getBalance() < rawAmount) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		} else if (rawAmount <= 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-transfer-negative-or-zero")).queue();
			return;
		} else if (from.getLoan() > 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-transfer-with-loan")).queue();
			return;
		}

		to.addCredit(liquidAmount, this.getClass());
		from.removeCredit(rawAmount, this.getClass());

		AccountDAO.saveAccount(to);
		AccountDAO.saveAccount(from);

		if (victorious)
			channel.sendMessage(":white_check_mark: | **" + liquidAmount + "** créditos transferidos com sucesso! (Exceed vitorioso isento de taxa)").queue();
		else
			channel.sendMessage(":white_check_mark: | **" + liquidAmount + "** créditos transferidos com sucesso! (Taxa de transferência: " + tax + " créditos)").queue();
	}
}