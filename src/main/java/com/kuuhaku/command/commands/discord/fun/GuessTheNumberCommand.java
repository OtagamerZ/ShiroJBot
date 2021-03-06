/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.LeaderboardsDAO;
import com.kuuhaku.controller.postgresql.PStateDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Command(
		name = "adivinheonumero",
		aliases = {"guessthenumber", "gtn", "aon"},
		category = Category.FUN
)
public class GuessTheNumberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());

		int theValue = Helper.rng(100, false);

		channel.sendMessage("Já escolhi um número de 0 a 100, você tem 5 chances para tentar adivinhar!").queue();

		ShiroInfo.getShiroEvents().addHandler(guild, new SimpleMessageListener() {
			private final Consumer<Void> success = s -> close();
			private Future<?> timeout = channel.sendMessage("Acabou o tempo, o número escolhido por mim era **" + theValue + "**.").queueAfter(5, TimeUnit.MINUTES, msg -> success.accept(null));
			int chances = 4;

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (!event.getAuthor().getId().equals(author.getId()) || !event.getChannel().getId().equals(channel.getId()))
					return;

				String value = event.getMessage().getContentRaw();
				if (value.equalsIgnoreCase("desistir") || Helper.equalsAny(value.split(" ")[0].replaceFirst(prefix, ""), "adivinheonumero", "guessthenumber", "gtn", "aon")) {
					channel.sendMessage("Você desistiu, o número escolhido por mim era **" + theValue + "**.").queue();
					success.accept(null);
					timeout.cancel(true);
					timeout = null;
					return;
				} else if (!StringUtils.isNumeric(value) || Integer.parseInt(value) < 0 || Integer.parseInt(value) > 100) {
					channel.sendMessage(I18n.getString("err_invalid-number")).queue();
					return;
				}

				int guess = Integer.parseInt(value);
				int diff = Math.abs(theValue - guess);
				String hint;

				if (diff > 50) {
					hint = "muito longe d";
				} else if (diff > 25) {
					hint = "longe d";
				} else if (diff > 10) {
					hint = "um pouco próximo d";
				} else {
					hint = "próximo a";
				}


				if (guess == theValue) {
					int prize = Helper.clamp(Helper.rng(750, false), 350, 750);
					channel.sendMessage("Você acertou! Como prêmio você receberá **" + Helper.separate(prize) + "** créditos.").queue();
					acc.addCredit(prize, this.getClass());
					AccountDAO.saveAccount(acc);

					if (ExceedDAO.hasExceed(author.getId())) {
						PoliticalState ps = com.kuuhaku.controller.postgresql.PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
						ps.modifyInfluence(10);
						PStateDAO.savePoliticalState(ps);
					}

					success.accept(null);
					timeout.cancel(true);
					timeout = null;
					int lost = chances == 4 ? 0 : LeaderboardsDAO.getUserScore(author.getId(), GuessTheCardsCommand.class);
					LeaderboardsDAO.submit(author, GuessTheNumberCommand.class, chances - lost);
				} else {
					if (chances > 0) {
						channel.sendMessage("(" + chances + " chances restantes) | Você errou, esse número está " + hint + "o número escolhido por mim.").queue();
						chances--;
					} else {
						channel.sendMessage("Acabaram suas chances, o número escolhido por mim era **" + theValue + "**.").queue();
						success.accept(null);
						timeout.cancel(true);
						timeout = null;
						int lost = LeaderboardsDAO.getUserScore(author.getId(), GuessTheCardsCommand.class);
						if (lost > 0)
							LeaderboardsDAO.submit(author, GuessTheCardsCommand.class, -lost);
					}
				}
			}
		});
	}
}
