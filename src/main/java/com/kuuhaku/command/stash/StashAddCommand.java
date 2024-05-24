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
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "stash",
		path = "add",
		category = Category.MISC
)
@Signature("<card:word:r> <kind:word>[n,c]")
public class StashAddCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		System.out.println(1);
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getCapacity() <= 0) {
			event.channel().sendMessage(locale.get("error/insufficient_space")).queue();
			return;
		}

		System.out.println(2);
		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			String sug = Utils.didYouMean(args.getString("card").toUpperCase(), "SELECT id AS value FROM v_card_names");
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_card_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
			}
			return;
		}

		System.out.println(3);
		KawaiponCard kc = kp.getCard(card, args.getString("kind", "n").equalsIgnoreCase("c"));
		if (kc == null) {
			event.channel().sendMessage(locale.get("error/not_owned")).queue();
			return;
		}

		System.out.println(4);
		new StashedCard(kp, kc).save();
		event.channel().sendMessage(locale.get("success/card_stored")).queue();
	}
}
