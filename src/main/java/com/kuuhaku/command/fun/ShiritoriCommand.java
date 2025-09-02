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

import com.kuuhaku.Constants;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.game.Shiritori;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.SigPattern;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Command(
		name = "shiritori",
		category = Category.FUN
)
@Syntax(
		patterns = @SigPattern(id = "users", value = "(<@!?(\\d+)>(?=\\s|$))+"),
		value = "<users:custom:r>[users]"
)
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class ShiritoriCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		if (GameInstance.PLAYERS.containsKey(event.user().getId())) {
			event.channel().sendMessage(locale.get("error/in_game_self")).queue();
			return;
		}

		List<User> others = event.message().getMentions().getUsers();
		if (others.contains(event.user())) {
			event.channel().sendMessage(locale.get("error/cannot_play_with_self")).queue();
			return;
		}

		for (User other : others) {
			if (GameInstance.PLAYERS.containsKey(other.getId())) {
				event.channel().sendMessage(locale.get("error/in_game_target", other.getEffectiveName())).queue();
				return;
			}
		}

		Set<User> pending = new HashSet<>(others);
		try {
			Utils.confirm(locale.get("question/shiritori",
							Utils.properlyJoin(locale.get("str/and")).apply(others.stream().map(User::getAsMention).toList()),
							event.user().getAsMention()
					), event.channel(), w -> {
						if (pending.remove(w.getUser())) {
							event.channel().sendMessage(locale.get("str/match_accept", w.getUser().getEffectiveName())).queue();

							if (!pending.isEmpty()) return false;
						} else {
							return false;
						}

						Shiritori shi = new Shiritori(locale,
								Stream.concat(Stream.of(event.user()), others.stream())
										.map(User::getId)
										.sorted(Collections.reverseOrder())
										.toArray(String[]::new)
						);
						shi.start(event.guild(), event.channel())
								.whenComplete((v, e) -> {
									if (e instanceof GameReport rep && rep.getCode() == GameReport.INITIALIZATION_ERROR) {
										Constants.LOGGER.error(e, e);
										event.channel().sendMessage(locale.get("error/error", e)).queue();
									}
								});

						return true;
					}, others.toArray(User[]::new)
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
