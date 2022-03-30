/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.LeaderboardsDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.helpers.MathHelper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "confronto",
		aliases = {"faceoff", "highnoon", "duelo"},
		usage = "req_difficulty",
		category = Category.FUN
)
public class FaceoffCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar uma dificuldade.").queue();
			return;
		}

		try {
			int level = Integer.parseInt(args[0]);
			if (!MathHelper.between(level, 0, 4)) throw new NumberFormatException();

			int min = 100 + ((3 - level) * 75);
			int max = MathHelper.rng(700 - (level * 100));
			int time = min + max;
			AtomicLong start = new AtomicLong(0);
			AtomicBoolean win = new AtomicBoolean();
			AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>();

			Main.getEvents().addHandler(guild, new SimpleMessageListener(channel) {
				{
					Main.getInfo().setGameInProgress(mutex, author);
				}

				@Override
				public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
					if (win.get()) {
						if (!isClosed()) close();
						return;
					}

					if (!event.getAuthor().getId().equals(author.getId())) return;

					if (start.get() == 0) {
						close();
						channel.sendMessage("Você foi apressado demais e derrubou a arma no chão.").queue();
						return;
					}

					String value = event.getMessage().getContentRaw();
					if (value.equalsIgnoreCase("bang")) {
						win.set(true);
						close();
						timeout.get().cancel(true);
						timeout.set(null);

						long react = MathHelper.clamp(System.currentTimeMillis() - start.get(), min, time);
						int prize = (int) Math.round(100f * (level + 1) + (min * MathHelper.rng(250f * (level + 1)) / react));
						channel.sendMessage("Você ganhou com um tempo de reação de **" + react + " ms**. Seu prêmio é de **" + prize + " CR**!").queue();

						Account acc = Account.find(Account.class, author.getId());
						acc.addCredit(prize, this.getClass());
						acc.save();

						LeaderboardsDAO.submit(author, FaceoffCommand.class, (int) react);
					}
				}
			});

			RestAction<Message> rst = channel.sendMessage("Prepare-se, o duelo começará em 3 segundos (digite `bang` quando eu disser fogo)!")
					.delay(3, TimeUnit.SECONDS)
					.flatMap(s -> s.editMessage("Em suas marcas..."));

			rst = rst.delay(MathHelper.rng(500, 3000), TimeUnit.MILLISECONDS);

			if (level > 2 && MathHelper.chance(25))
				rst = rst.flatMap(s -> s.editMessage("AGUA! <:KEKW:837794089486254180>"))
						.delay(MathHelper.rng(500, 2000), TimeUnit.MILLISECONDS);

			if (level > 1 && MathHelper.chance(50))
				rst = rst.flatMap(s -> s.editMessage("Ainda não..."))
						.delay(MathHelper.rng(500, 1500), TimeUnit.MILLISECONDS);

			rst = rst.flatMap(s -> s.editMessage("**FOGO!**"));

			rst.queue(t -> {
				start.set(System.currentTimeMillis());
				timeout.set(Main.getInfo().getSchedulerPool().schedule(() -> {
							if (!win.get()) {
								win.set(true);
								channel.sendMessage(":gun: | BANG! Você perdeu.").complete();
							}
						}, time, TimeUnit.MILLISECONDS
				));
			});
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Dificuldade inválida, ela deve ser um valor entre 0 (fácil) e 3 (difícil).").queue();
		}
	}
}
