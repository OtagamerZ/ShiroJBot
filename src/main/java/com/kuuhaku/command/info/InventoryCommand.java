/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.info;

import com.github.ygimenez.model.Page;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.collections4.bag.HashBag;

import java.util.List;

@Command(
		name = "items",
		category = Category.INFO
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class InventoryCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		HashBag<UserItem> items = acc.getItems();
		if (items.isEmpty()) {
			event.channel().sendMessage(locale.get("error/inventory_empty")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/items_available"));

		List<Page> pages = Utils.generatePages(eb, items, 20, 10,
				i -> {
					String out = i.toString(locale);
					if (i.getStackSize() > 0) {
						out += "\n" + locale.get("str/item_has", items.getCount(i) + "/" + i.getStackSize());
					} else {
						out += "\n" + locale.get("str/item_has", items.getCount(i));
					}

					if (i.isPassive()) {
						out += " | **" + locale.get("str/passive") + "**";
					}

					out += "\n" + i.getDescription(locale);

					return out + "\n";
				},
				(p, t) -> eb.setFooter(locale.get("str/page", p + 1, t))
		);

		Utils.paginate(pages, 1, true, event.channel(), event.user());
	}
}
