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
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

@Command(
		name = "transfer",
		path = "gear",
		category = Category.STAFF
)
@Syntax("<user:user:r> <gear:number:r>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class TransferGearCommand implements Executable {
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

		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Deck tgt = DAO.find(Account.class, target.getId()).getDeck();
		if (tgt == null) {
			event.channel().sendMessage(locale.get("error/no_deck_target", target.getAsMention(), data.config().getPrefix())).queue();
			return;
		}

		Hero from = d.getHero();
		if (from == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		Hero to = tgt.getHero();
		if (to == null) {
			event.channel().sendMessage(locale.get("error/no_hero_target", target.getAsMention(), data.config().getPrefix())).queue();
			return;
		} else if (to.getInventory().size() > to.getInventoryCapacity()) {
			event.channel().sendMessage(locale.get("error/overburdened_target", to.getName())).queue();
			return;
		}

		Gear g = from.getInvGear(args.getInt("gear"));
		if (g == null) {
			event.channel().sendMessage(locale.get("error/gear_not_found")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/transfer", g.getName(locale), target.getName()), event.channel(), w -> {
						g.setOwner(to);
						g.save();

						event.channel().sendMessage(locale.get("success/transfer")).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
