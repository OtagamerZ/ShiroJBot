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
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.Stash;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;

@Command(
		name = "store",
		category = Category.MISC
)
@Signature({"<card:word:r> <kind:word>[n,f]"})
public class StoreCardCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
        Stash stash = DAO.find(Stash.class, event.user().getId());
        if (stash.getCapacity() <= 0) {
            event.channel().sendMessage(locale.get("error/stash_full")).queue();
            return;
        }

		Kawaipon kp = stash.getAccount().getKawaipon();
		KawaiponCard card = kp.getCards().stream()
				.filter(kc -> kc.getCard().getId().equalsIgnoreCase(args.getString("card")))
				.filter(kc -> kc.isFoil() == args.getString("kind", "n").equalsIgnoreCase("f"))
				.findFirst().orElse(null);

		if (card == null) {
			event.channel().sendMessage(locale.get("error/not_owned")).queue();
			return;
		}

		card.store();
		event.channel().sendMessage(locale.get("success/card_stored")).queue();
	}
}
