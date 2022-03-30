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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.helpers.StringHelper;
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
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage(I18n.getString("err_transfer-no-amount")).queue();
			return;
		} else if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage(I18n.getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage(I18n.getString("err_cannot-transfer-to-yourself")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
			return;
		}

		Account from = Account.find(Account.class, author.getId());
		Account to = Account.find(Account.class, message.getMentionedUsers().get(0).getId());

		int value = Integer.parseInt(args[1]);

		if (from.getBalance() < value) {
			channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
			return;
		} else if (value <= 0) {
			channel.sendMessage(I18n.getString("err_cannot-transfer-negative-or-zero")).queue();
			return;
		}

		to.addCredit(value, this.getClass());
		from.removeCredit(value, this.getClass());

		to.save();
		from.save();

		channel.sendMessage("✅ | **" + StringHelper.separate(value) + "** CR transferidos com sucesso!").queue();
	}
}