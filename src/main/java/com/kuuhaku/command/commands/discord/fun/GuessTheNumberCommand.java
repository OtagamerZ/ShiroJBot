/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GuessTheNumberCommand extends Command {

	public GuessTheNumberCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public GuessTheNumberCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public GuessTheNumberCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public GuessTheNumberCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());

		int theValue = Helper.rng(100, false);

		channel.sendMessage("Já escolhi um número de 0 a 100, você tem 5 chances para tentar adivinhar!").queue();

		Main.getInfo().getShiroEvents().addHandler(guild, new SimpleMessageListener() {
			private final Consumer<Void> success = s -> close();
			private Future<?> timeout = channel.sendMessage("Acabou o tempo, o número escolhido por mim era **" + theValue + "**.").queueAfter(5, TimeUnit.MINUTES, msg -> success.accept(null));
			int chances = 4;

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (!event.getAuthor().getId().equals(author.getId()) || !event.getChannel().getId().equals(channel.getId()))
					return;

				String value = event.getMessage().getContentRaw();
				if (value.equalsIgnoreCase("desistir") || Helper.equalsAny(prefix + rawCmd.split(" ")[0], ArrayUtils.addAll(getAliases(), getName()))) {
					channel.sendMessage("Você desistiu, o número escolhido por mim era **" + theValue + "**.").queue();
					success.accept(null);
					timeout.cancel(true);
					timeout = null;
					return;
				} else if (!StringUtils.isNumeric(value) || Integer.parseInt(value) < 0 || Integer.parseInt(value) > 100) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-number")).queue();
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
					channel.sendMessage("Você acertou! Como prêmio você receberá **" + prize + "** créditos.").queue();
					acc.addCredit(prize, this.getClass());
					AccountDAO.saveAccount(acc);

					if (ExceedDAO.hasExceed(author.getId())) {
						PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
						ps.modifyInfluence(10);
						PStateDAO.savePoliticalState(ps);
					}

					success.accept(null);
					timeout.cancel(true);
					timeout = null;
				} else {
					if (chances > 0) {
						channel.sendMessage("(" + chances + " chances restantes) | Você errou, esse número está " + hint + "o número escolhido por mim.").queue();
						chances--;
					} else {
						channel.sendMessage("Acabaram suas chances, o número escolhido por mim era **" + theValue + "**.").queue();
						success.accept(null);
						timeout.cancel(true);
						timeout = null;
					}
				}
			}
		});
	}
}
