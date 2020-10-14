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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AuctionCommand extends Command {

	public AuctionCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AuctionCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AuctionCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AuctionCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 3) {
			channel.sendMessage("❌ | Você precisa informar a carta, o tipo dela e o valor inicial para fazer um leilão.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[2])) {
			channel.sendMessage("❌ | O preço precisa ser um valor inteiro.").queue();
			return;
		} else if (!Helper.equalsAny(args[1], "N", "C")) {
			channel.sendMessage("❌ | Você precisa informar o tipo da carta que deseja leiloar (`N` = normal, `C` = cromada).").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Card c = CardDAO.getCard(args[0], false);

		boolean foil = args[1].equalsIgnoreCase("C");

		if (c == null) {
			channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[0], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
			return;
		}

		KawaiponCard card = kp.getCard(c, foil);

		if (card == null) {
			channel.sendMessage("❌ | Você não pode leiloar uma carta que não possui!").queue();
			return;
		}

		try {
			boolean hasLoan = AccountDAO.getAccount(kp.getUid()).getLoan() > 0;
			int price = Integer.parseInt(args[2]);
			int min = c.getRarity().getIndex() * (hasLoan ? Helper.BASE_CARD_PRICE * 2 : Helper.BASE_CARD_PRICE / 2) * (foil ? 2 : 1);

			if (price < min) {
				if (hasLoan)
					channel.sendMessage("❌ | Como você possui uma dívida ativa, você não pode leiloar essa carta por menos que " + min + " créditos.").queue();
				else
					channel.sendMessage("❌ | Você não pode leiloar essa carta por menos que " + min + " créditos.").queue();
				return;
			}

			ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

			AtomicReference<Future<?>> event = new AtomicReference<>();

			AtomicInteger phase = new AtomicInteger(1);
			AtomicReference<Pair<User, Integer>> highest = new AtomicReference<>(null);
			Runnable auct = () -> {
				if (phase.get() == 4 && highest.get() != null) {
					channel.sendMessage("**Carta vendida** para " + highest.get().getLeft().getAsMention() + " por **" + highest.get().getRight() + "** créditos!").queue();

					Kawaipon k = KawaiponDAO.getKawaipon(author.getId());
					k.removeCard(card);
					KawaiponDAO.saveKawaipon(k);

					Kawaipon buyer = KawaiponDAO.getKawaipon(highest.get().getLeft().getId());
					buyer.addCard(card);
					KawaiponDAO.saveKawaipon(buyer);

					Account acc = AccountDAO.getAccount(author.getId());
					acc.addCredit(highest.get().getRight(), this.getClass());
					AccountDAO.saveAccount(acc);

					Main.getInfo().getConfirmationPending().invalidate(author.getId());
				} else {
					switch (phase.get()) {
						case 1 -> channel.sendMessage("Dou-lhe 1...").queue();
						case 2 -> channel.sendMessage("""
								Dou-lhe 2...
								Vamos lá pessoal, será que eu ouvi um %s?
								""".formatted(highest.get().getRight() + 250)).queue();
						case 3 -> channel.sendMessage("Dou-lhe 3...").queue();
					}

					phase.getAndIncrement();
				}
			};

			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Esta carta será vendida para quer der o maior valor. Deseja mesmo leiloá-la?").queue(s -> {
				Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
					if (!ShiroInfo.getHashes().remove(hash)) return;
					if (mb.getId().equals(author.getId())) {
						event.set(channel.sendMessage("Não houve nenhuma oferta, declaro o leilão **encerrado**!").queueAfter(30, TimeUnit.SECONDS, msg ->
								Main.getInfo().getConfirmationPending().invalidate(author.getId())
						));

						s.delete().flatMap(d -> channel.sendMessage(":white_check_mark: | Leilão aberto com sucesso, se não houver ofertas maiores que " + price + " dentro de 30 segundos irei fechá-lo!")).queue();
						Main.getInfo().getAPI().addEventListener(new SimpleMessageListener() {
							@Override
							public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent evt) {
								String raw = evt.getMessage().getContentRaw();
								if (StringUtils.isNumeric(raw)) {
									int offer = Integer.parseInt(raw);

									if (highest.get() == null || offer > highest.get().getRight()) {
										highest.set(Pair.of(evt.getAuthor(), offer));
										phase.set(1);

										channel.sendMessage(evt.getAuthor().getAsMention() + " ofereceu **" + offer + " créditos**!").queue();

										event.get().cancel(true);
										event.set(exec.schedule(auct, 5, TimeUnit.SECONDS));
									}
								}
							}
						});
					}
				}), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()), ms -> {
					ShiroInfo.getHashes().remove(hash);
					Main.getInfo().getConfirmationPending().invalidate(author.getId());
				});
			});
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O valor máximo é " + Integer.MAX_VALUE + " créditos!").queue();
		}
	}
}
