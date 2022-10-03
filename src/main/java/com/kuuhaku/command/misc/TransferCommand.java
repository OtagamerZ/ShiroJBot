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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Command(
		name = "transfer",
		category = Category.MISC
)
@Signature({
		"<user:user:r> <value:number:r>",
		"<user:user:r> <card:text:r>"
})
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class TransferCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		User target = event.message().getMentionedUsers().get(0);

		if (args.has("value")) {
			Account acc = data.profile().getAccount();

			int value = args.getInt("value");
			if (!acc.hasEnough(value, Currency.CR)) {
				event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
				return;
			}

			try {
				Utils.confirm(locale.get("question/transfer", Utils.separate(value) + " ₵R", target.getName()), event.channel(), w -> {
							acc.consumeCR(value, "Transferred to " + target.getName());
							DAO.find(Account.class, target.getId()).addCR(value, "Received from " + event.user().getName());

							event.channel().sendMessage(locale.get("success/transfer")).queue();
							return true;
						}, event.user()
				);
			} catch (PendingConfirmationException e) {
				event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
			}
		} else {
			Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
			if (kp.getStash().isEmpty()) {
				event.channel().sendMessage(locale.get("error/empty_stash")).queue();
				return;
			}

			Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
			if (card == null) {
				List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card");

				Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
				event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
				return;
			}

			CompletableFuture<StashedCard> select = new CompletableFuture<>();
			Utils.selectOption(locale, event.channel(), kp.getNotInUse(), card, event.user())
					.thenAccept(sc -> {
						if (sc == null) {
							event.channel().sendMessage(locale.get("error/invalid_value")).queue();
							select.complete(null);
							return;
						}

						select.complete(sc);
					})
					.exceptionally(t -> {
						if (!(t.getCause() instanceof NoResultException)) {
							Constants.LOGGER.error(t, t);
						}

						event.channel().sendMessage(locale.get("error/not_owned")).queue();
						select.complete(null);
						return null;
					});

			try {
				StashedCard selected = select.get();
				if (selected == null) {
					event.channel().sendMessage(locale.get("error/invalid_value")).queue();
					return;
				}

				Utils.confirm(locale.get("question/transfer", selected, target.getName()), event.channel(), w -> {
							selected.setKawaipon(DAO.find(Kawaipon.class, target.getId()));
							selected.save();

							event.channel().sendMessage(locale.get("success/transfer")).queue();
					return true;
						}, event.user()
				);
			} catch (InterruptedException | ExecutionException ignore) {
			} catch (PendingConfirmationException e) {
				event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
			}
		}
	}
}