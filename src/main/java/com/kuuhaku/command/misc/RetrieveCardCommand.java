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

package com.kuuhaku.command.misc;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.Stash;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;

import java.util.List;

@Command(
		name = "retrieve",
		category = Category.MISC
)
@Signature({"<card:word:r>"})
public class RetrieveCardCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
        Stash stash = DAO.find(Stash.class, event.user().getId());

		Card card = DAO.find(Card.class, args.getString("card"));
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM Card");

			String sug = Utils.didYouMean(args.getString("card"), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
			return;
		}

		Utils.selectOption(locale, event.channel(), stash, card)
				.thenAccept(sc -> {
					Kawaipon kp = stash.getAccount().getKawaipon();

					switch (sc.getType()) {
						case KAWAIPON -> {
							KawaiponCard kc = kp.getCards().stream()
									.filter(c -> c.getCard().getId().equalsIgnoreCase(args.getString("card")))
									.filter(c -> c.isFoil() == args.getString("kind", "n").equalsIgnoreCase("f"))
									.findFirst().orElse(null);

							if (kc != null) {
								event.channel().sendMessage(locale.get("error/in_collection")).queue();
								return;
							}

							new KawaiponCard(sc.getUUID(), kp, card, sc.isFoil()).save();
						}
						case EVOGEAR -> {
							//TODO
						}
						case FIELD -> {
							//TODO
						}
					}

					sc.delete();
					event.channel().sendMessage(locale.get("success/card_retrieved")).queue();
				})
				.exceptionally(t -> {
					event.channel().sendMessage("error/not_owned").queue();
					return null;
				});
	}
}
