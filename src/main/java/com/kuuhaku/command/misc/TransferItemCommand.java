/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.misc;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.Map;

@Command(
		name = "transfer",
		path = "item",
		category = Category.MISC
)
@Syntax("<user:user:r> <item:word:r> <amount:number>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class TransferItemCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		User target = event.users(0);
		if (target == null) {
			event.channel().sendMessage(locale.get("error/invalid_mention")).queue();
			return;
		} else if (target.equals(event.user())) {
			event.channel().sendMessage(locale.get("error/self_not_allowed")).queue();
			return;
		}

		Account acc = data.profile().getAccount();
		Map<UserItem, Integer> items = acc.getItems();
		if (items.isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_inventory")).queue();
			return;
		}

		UserItem item = items.keySet().parallelStream()
				.filter(i -> i.getId().equals(args.getString("item").toUpperCase()))
				.findAny().orElse(null);

		int qtd = args.getInt("amount", 1);
		if (qtd < 1) {
			event.channel().sendMessage(locale.get("error/invalid_value_low", 1)).queue();
			return;
		}

		if (item == null) {
			List<String> names = items.keySet().stream().map(UserItem::getId).toList();

			String sug = Utils.didYouMean(args.getString("item").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/item_not_found", sug)).queue();
			return;
		} else if (!items.containsKey(item)) {
			event.channel().sendMessage(locale.get("error/item_not_have")).queue();
			return;
		} else if (item.isAccountBound()) {
			event.channel().sendMessage(locale.get("error/item_account_bound")).queue();
			return;
		} else if (acc.getItemCount(item.getId()) < qtd) {
			event.channel().sendMessage(locale.get("error/item_not_enough")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/transfer", item.getInfo(locale).getName(), target.getName()), event.channel(), w -> {
						if (acc.hasChanged()) {
							event.channel().sendMessage(locale.get("error/account_state_changed")).queue();
							return true;
						}

						acc.addItem(item, -qtd);

						Account tgt = DAO.find(Account.class, target.getId());
						tgt.addItem(item, qtd);

						event.channel().sendMessage(locale.get("success/transfer")).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
