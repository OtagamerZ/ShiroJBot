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

package com.kuuhaku.command.stash;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.util.List;
import java.util.Set;

@Command(
		name = "stash",
		path = {"add", "all"},
		category = Category.MISC
)
public class StashAddAllCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());

		Set<StashedCard> addable = kp.getCollection();
		if (addable.isEmpty()) {
			event.channel().sendMessage(locale.get("error/kawaipon_empty")).queue();
			return;
		} else if (addable.size() > kp.getCapacity()) {
			event.channel().sendMessage(locale.get("error/no_stash_space", addable.size() - kp.getCapacity())).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/stash_add_all", addable.size()), event.channel(), w -> {
						List<String> uuids = addable.stream()
								.map(StashedCard::getUUID)
								.toList();

						DAO.applyNative(StashedCard.class, "UPDATE stashed_card SET in_collection = FALSE WHERE uuid IN ?1", uuids);
						event.channel().sendMessage(locale.get("success/cards_stored")).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
