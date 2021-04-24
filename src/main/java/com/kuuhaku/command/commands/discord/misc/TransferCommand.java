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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.LotteryDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.LotteryValue;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "transferir",
		aliases = {"transfer", "tr"},
		usage = "req_user-amount",
		category = Category.MISC
)
public class TransferCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_transfer-no-amount")).queue();
			return;
		} else if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-transfer-to-yourself")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-amount")).queue();
			return;
		}


		Account from = AccountDAO.getAccount(author.getId());
		Account to = AccountDAO.getAccount(message.getMentionedUsers().get(0).getId());

		boolean victorious = ExceedDAO.hasExceed(author.getId()) && Main.getInfo().getWinner().equals(ExceedDAO.getExceed(author.getId()));
		int rawAmount = Integer.parseInt(args[1]);
		double tax = 0.01 + Helper.clamp(0.29 * Helper.offsetPrcnt(from.getBalance(), 500000, 100000), 0, 0.29);
		int liquidAmount = rawAmount - (victorious ? 0 : (int) Math.floor(rawAmount * tax));

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

		LotteryValue lv = LotteryDAO.getLotteryValue();
		lv.addValue(rawAmount - liquidAmount);
		LotteryDAO.saveLotteryValue(lv);

		AccountDAO.saveAccount(to);
		AccountDAO.saveAccount(from);

		if (victorious)
			channel.sendMessage("✅ | **" + Helper.separate(liquidAmount) + "** créditos transferidos com sucesso! (Exceed vitorioso isento de taxa)").queue();
		else
			channel.sendMessage("✅ | **" + Helper.separate(liquidAmount) + "** créditos transferidos com sucesso! (Taxa de transferência: " + Helper.roundToString(tax * 100, 1) + "%)").queue();
	}
}