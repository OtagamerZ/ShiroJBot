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

package com.kuuhaku.command.stash;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.List;

@Command(
		name = "stash",
		subname = "trash",
		category = Category.MISC
)
@Signature("<action:word:r>[add,remove] <card:word:r> <confirm:word>[y]")
public class StashTrashCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStash().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card WHERE rarity NOT IN ('ULTIMATE', 'NONE')");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		boolean add = args.getString("action").equals("add");

		Utils.selectOption(args.has("confirm"), locale, event.channel(), add ? kp.getNotInUse() : kp.getTrash(), card, event.user())
				.thenAccept(sc -> {
					if (sc == null) {
						event.channel().sendMessage(locale.get("error/invalid_value")).queue();
						return;
					}

					sc.setTrash(add);
					sc.save();

					if (sc.isTrash()) {
						event.channel().sendMessage(locale.get("success/card_trash")).queue();
					} else {
						event.channel().sendMessage(locale.get("success/card_untrash")).queue();
					}
				})
				.exceptionally(t -> {
					if (!(t.getCause() instanceof NoResultException)) {
						Constants.LOGGER.error(t, t);
					}

					event.channel().sendMessage(locale.get("error/not_owned")).queue();
					return null;
				});
	}
}