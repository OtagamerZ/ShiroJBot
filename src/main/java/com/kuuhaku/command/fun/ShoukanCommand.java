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

package com.kuuhaku.command.fun;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingFunction;
import com.kuuhaku.Constants;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Arcade;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Command(
		name = "shoukan",
		category = Category.FUN
)
@Signature(allowEmpty = true, value = {
		"<user:user:r> <arcade:word>",
		"<arcade:word>"
})
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class ShoukanCommand implements Executable {
	private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		if (GameInstance.PLAYERS.contains(event.user().getId())) {
			event.channel().sendMessage(locale.get("error/in_game_self")).queue();
			return;
		}

		User other;
		if (args.has("user")) {
			other = event.users(0);
			if (other == null) {
				event.channel().sendMessage(locale.get("error/invalid_mention", 0)).queue();
				return;
			}
		} else {
			other = event.user();
		}

		if (GameInstance.PLAYERS.contains(other.getId())) {
			event.channel().sendMessage(locale.get("error/in_game_target", other.getEffectiveName())).queue();
			return;
		}

		try {
			Arcade arcade = args.getEnum(Arcade.class, "arcade");
			ThrowingFunction<ButtonWrapper, Boolean> act = w -> {
				Message m = Pages.subGet(event.channel().sendMessage(Constants.LOADING.apply(locale.get("str/loading_game", getRandomTip(locale)))));

				try {
					Shoukan skn = new Shoukan(locale, arcade, event.user(), other);
					skn.start(event.guild(), event.channel())
							.whenComplete((v, e) -> {
								if (e instanceof GameReport rep && rep.getCode() == GameReport.INITIALIZATION_ERROR) {
									event.channel().sendMessage(locale.get("error/error", e)).queue();
									Constants.LOGGER.error(e, e);
								}

								for (String s : skn.getPlayers()) {
									GameInstance.PLAYERS.remove(s);
								}
							});

					updateTip(locale, skn, m);
				} catch (GameReport e) {
					switch (e.getCode()) {
						case GameReport.NO_DECK -> {
							if (e.getContent().equals(event.user().getId())) {
								event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
							} else {
								event.channel().sendMessage(locale.get("error/no_deck_target", "<@" + e.getContent() + ">", data.config().getPrefix())).queue();
							}
						}
						case GameReport.INVALID_DECK -> {
							if (e.getContent().equals(event.user().getId())) {
								event.channel().sendMessage(locale.get("error/invalid_deck", data.config().getPrefix())).queue();
							} else {
								event.channel().sendMessage(locale.get("error/invalid_deck_target", "<@" + e.getContent() + ">", data.config().getPrefix())).queue();
							}
						}
					}

					m.delete().queue(null, Utils::doNothing);
				}

				return true;
			};

			if (other.equals(event.user())) {
				act.apply(null);
			} else {
				if (arcade != null) {
					Utils.confirm(locale.get("question/shoukan_arcade", other.getAsMention(), event.user().getAsMention(), arcade.toString(locale)), event.channel(), act, other);
					return;
				}

				Utils.confirm(locale.get("question/shoukan", other.getAsMention(), event.user().getAsMention()), event.channel(), act, other);
			}
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}

	private String getRandomTip(I18N locale) {
		return locale.get("str/loading_tip_" + Calc.rng(7));
	}

	private void updateTip(I18N locale, Shoukan skn, Message m) {
		exec.schedule(() -> {
			if (!skn.isInitialized()) {
				m.editMessage(Constants.LOADING.apply(locale.get("str/loading_game", getRandomTip(locale)))).queue(null, Utils::doNothing);
				updateTip(locale, skn, m);
				return;
			}

			m.delete().queue(null, Utils::doNothing);
		}, Calc.rng(3000, 5000), TimeUnit.MILLISECONDS);
	}
}
