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

import com.kuuhaku.exceptions.ItemUseException;
import com.kuuhaku.exceptions.PassiveItemException;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.SyntaxParser;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.util.List;
import java.util.Map;

@Command(
		name = "items",
		path = "use",
		category = Category.MISC
)
@Syntax(allowEmpty = true, value = "<id:word:r> <args:text>")
public class UseItemCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Map<UserItem, Integer> items = acc.getItems();
		UserItem item = items.keySet().parallelStream()
				.filter(i -> i.getId().equals(args.getString("id").toUpperCase()))
				.findAny().orElse(null);

		if (item == null) {
			List<String> names = items.keySet().stream().map(UserItem::getId).toList();

			String sug = Utils.didYouMean(args.getString("id").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/item_not_found", sug)).queue();
			return;
		} else if (!items.containsKey(item)) {
			event.channel().sendMessage(locale.get("error/item_not_have")).queue();
			return;
		} else if (item.isPassive()) {
			event.channel().sendMessage(locale.get("error/item_not_usable")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/item_use", item.getName(locale), items.getOrDefault(item, 0)), event.channel(), w -> {
						if (acc.hasChanged()) {
							event.channel().sendMessage(locale.get("error/account_state_changed")).queue();
							return true;
						}

						try {
							JSONObject params;
							if (item.getSyntax() == null) params = new JSONObject();
							else {
								params = SyntaxParser.parse(locale, new String[]{item.getSyntax()}, null, false, args.getString("args"));
							}

							item.execute(locale, event.channel(), data.profile().refresh(), params);
						} catch (PassiveItemException e) {
							event.channel().sendMessage(locale.get("error/item_not_usable")).queue();
							return true;
						} catch (ItemUseException e) {
							String out = locale.get(e.getMessage(), e.getArgs());
							if (out.isBlank() || out.equalsIgnoreCase(e.getMessage())) {
								out = LocalizedString.get(locale, e.getMessage(), "").formatted(e.getArgs());
							}

							event.channel().sendMessage(Utils.getOr(out, e.getMessage())).queue();
							return true;
						} catch (Exception e) {
							event.channel().sendMessage(locale.get("error/item_invalid_args")).queue();
							return true;
						}

						if (!acc.consumeItem(item)) {
							event.channel().sendMessage(locale.get("error/item_not_have")).queue();
							return true;
						}

						event.channel().sendMessage(locale.get("success/item_use", item.getName(locale))).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
