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

import com.kuuhaku.Constants;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.game.Shiritori;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Command(
		name = "shiritori",
		category = Category.FUN
)
@Signature("<users:user:r>")
@Requires(Permission.MESSAGE_ATTACH_FILES)
public class ShiritoriCommand implements Executable {
	private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);

	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		if (GameInstance.PLAYERS.contains(event.user().getId())) {
			event.channel().sendMessage(locale.get("error/in_game_self")).queue();
			return;
		}

		List<Member> others = event.message().getMentionedMembers();
		others.remove(event.member());

		for (Member other : others) {
			if (GameInstance.PLAYERS.contains(other.getId())) {
				event.channel().sendMessage(locale.get("error/in_game_target", other.getEffectiveName())).queue();
				return;
			}
		}

		Set<Member> pending = new HashSet<>(others);
		try {
			Utils.confirm(locale.get("question/shiritori",
							Utils.properlyJoin(locale.get("str/and")).apply(others.stream().map(Member::getAsMention).toList()),
							event.user().getAsMention()
					), event.channel(), w -> {
						if (!pending.isEmpty()) {
							event.channel().sendMessage(locale.get("str/match_accept", w.getMember().getEffectiveName())).queue();
							pending.remove(w.getMember());
							return false;
						}

						try {
							Shiritori shi = new Shiritori(locale, others.stream().map(Member::getId).toArray(String[]::new));
							shi.start(event.guild(), event.channel())
									.whenComplete((v, e) -> {
										if (e instanceof GameReport rep && rep.getCode() == 1) {
											event.channel().sendMessage(locale.get("error/error", e)).queue();
											Constants.LOGGER.error(e, e);
										}

										for (String s : shi.getPlayers()) {
											GameInstance.PLAYERS.remove(s);
										}
									});
						} catch (GameReport e) {
							if (e.getContent().equals(event.user().getId())) {
								event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
							} else {
								event.channel().sendMessage(locale.get("error/no_deck_target", "<@" + e.getContent() + ">", data.config().getPrefix())).queue();
							}
						}

						return true;
					}, others.stream().map(Member::getUser).toArray(User[]::new)
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
