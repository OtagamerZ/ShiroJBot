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
import net.dv8tion.jda.api.requests.RestAction;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Command(
		name = "confronto",
		aliases = {"standoff", "highnoon", "duelo"},
		usage = "req_difficulty",
		category = Category.FUN
)
public class StandoffCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar uma dificuldade.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		try {
			int level = Integer.parseInt(args[0]);
			if (!Helper.between(level, 0, 4)) throw new NumberFormatException();

			int min = 100 + ((3 - level) * 75);
			int max = Helper.rng(700 - (level * 100), false);
			int time = min + max;
			AtomicLong start = new AtomicLong(0);

			RestAction<Message> rst = channel.sendMessage("Prepare-se, o duelo começará em 3 segundos (digite `bang` quando eu disser fogo)!")
					.delay(3, TimeUnit.SECONDS)
					.flatMap(s -> s.editMessage("Em suas marcas..."));

			if (level > 1 && Helper.chance(50)) {
				rst = rst.delay(500 + Helper.rng(1500, false), TimeUnit.MILLISECONDS)
						.flatMap(s -> s.editMessage("Ainda não..."))
						.delay(500 + Helper.rng(2500, false), TimeUnit.MILLISECONDS)
						.flatMap(s -> s.editMessage("**FOGO!**"));
			} else {
				rst = rst.delay(500 + Helper.rng(4500, false), TimeUnit.MILLISECONDS)
						.flatMap(s -> s.editMessage("**FOGO!**"));
			}

			rst.queue(t -> {
				start.set(System.currentTimeMillis());
				ShiroInfo.getShiroEvents().addHandler(guild, new SimpleMessageListener() {
					private final Consumer<Void> success = s -> close();
					private final AtomicBoolean win = new AtomicBoolean();
					private ScheduledFuture<?> timeout = Main.getInfo().getScheduler().schedule(() -> {
								if (!win.get()) {
									success.accept(null);
									channel.sendMessage(":gun: BANG! Você perdeu..").complete();
								}
							}, time, TimeUnit.MILLISECONDS
					);

					@Override
					public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
						if (!event.getAuthor().getId().equals(author.getId()) || !event.getChannel().getId().equals(channel.getId()))
							return;

						String value = event.getMessage().getContentRaw();
						if (!value.equalsIgnoreCase("bang")) {
							channel.sendMessage("Você errou o gatilho.").queue();
							success.accept(null);
							timeout.cancel(true);
							timeout = null;
							return;
						}

						if (!win.get()) {
							win.set(true);
							long react = Helper.clamp(System.currentTimeMillis() - start.get(), min, time);
							success.accept(null);
							timeout.cancel(true);
							timeout = null;

							int prize = (int) Math.round(min * Helper.rng(500f * (level + 1)) / react);
							channel.sendMessage("Você ganhou com um tempo de reação de **" + react + " ms**. Seu prêmio é de **" + prize + " créditos**!").queue();
							acc.addCredit(prize, this.getClass());
							AccountDAO.saveAccount(acc);

							if (ExceedDAO.hasExceed(author.getId())) {
								PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
								ps.modifyInfluence(10 * level);
								PStateDAO.savePoliticalState(ps);
							}

							LeaderboardsDAO.submit(author, StandoffCommand.class, (int) react);
						}
					}
				});
			});
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Dificuldade inválida, ela deve ser um valor entre 0 (fácil) e 3 (difícil).").queue();
		}
	}
}
