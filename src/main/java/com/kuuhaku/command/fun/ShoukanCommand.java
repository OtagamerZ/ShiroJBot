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

package com.kuuhaku.command.fun;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Constants;
import com.kuuhaku.games.Shoukan;
import com.kuuhaku.games.engine.GameException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.Calc;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Command(
		name = "shoukan",
		category = Category.FUN
)
@Signature("<user:user:r>")
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class ShoukanCommand implements Executable {
	private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);

	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Shoukan skn = new Shoukan(locale, event.user(), event.message().getMentionedUsers().get(0));
		Message m = Pages.subGet(event.channel().sendMessage(locale.get("str/loading_game", getRandomTip(locale))));
		skn.start(event.guild(), event.channel())
				.handle((v, err) -> {
					if (err == null) {
						event.channel().sendMessage("Done").queue();
					} else if (err instanceof GameException) {
						event.channel().sendMessage("Error").queue();
					} else {
						Constants.LOGGER.error(err, err);
					}

					return null;
				});

		updateTip(locale, skn, m);
	}

	private String getRandomTip(I18N locale) {
		return locale.get("str/loading_tip_" + Calc.rng(5));
	}

	private void updateTip(I18N locale, Shoukan skn, Message m) {
		exec.schedule(() -> {
			if (!skn.isInitialized()) {
				m.editMessage(locale.get("str/loading_game", getRandomTip(locale))).queue(null, Utils::doNothing);
				updateTip(locale, skn, m);
				return;
			}

			m.delete().queue(null, Utils::doNothing);
			throw new RuntimeException("Done");
		}, Calc.rng(2000, 4000), TimeUnit.MILLISECONDS);
	}
}
