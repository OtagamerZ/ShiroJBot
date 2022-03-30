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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "leilao",
		aliases = {"auction", "auct"},
		usage = "req_card-price",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class AuctionCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 3) {
			channel.sendMessage("❌ | Você precisa informar a carta, o tipo dela e o valor inicial para fazer um leilão.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[2])) {
			channel.sendMessage("❌ | O preço precisa ser um valor inteiro.").queue();
			return;
		}

		int type = switch (args[1].toUpperCase(Locale.ROOT)) {
			case "N", "C" -> 1;
			case "E" -> 2;
			case "F" -> 3;
			default -> -1;
		};

		if (type == -1) {
			channel.sendMessage("❌ | Você precisa informar o tipo da carta que deseja leiloar (`N` = normal, `C` = cromada, `E` = evogear, `F` = campo).").queue();
			return;
		}

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Deck dk = kp.getDeck();
		Object obj;
		boolean foil = args[1].equalsIgnoreCase("C");
		switch (type) {
			case 1 -> {
				Card c = CardDAO.getCard(args[0], false);

				if (c == null) {
					channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + StringHelper.didYouMean(args[0], Card.getCards().stream().map(Card::getId).toArray(String[]::new)) + "`?").queue();
					return;
				}

				KawaiponCard card = kp.getCard(c, foil);

				if (card == null) {
					channel.sendMessage("❌ | Você não pode leiloar uma carta que não possui!").queue();
					return;
				}

				obj = card;
			}
			case 2 -> {
				Evogear c = Evogear.getEvogear(args[0]);
				if (c == null) {
					channel.sendMessage("❌ | Esse equipamento não existe, você não quis dizer `" + StringHelper.didYouMean(args[0], Evogear.getEvogears().stream().map(d -> d.getCard().getId()).toList()) + "`?").queue();
					return;
				} else if (!dk.getEquipments().contains(c)) {
					channel.sendMessage("❌ | Você não pode leiloar um equipamento que não possui!").queue();
					return;
				}

				obj = c;
			}
			default -> {
				Field f = Field.getField(args[0]);

				if (f == null) {
					channel.sendMessage("❌ | Essa arena não existe, você não quis dizer `" + StringHelper.didYouMean(args[0], Field.getFields().stream().map(d -> d.getCard().getId()).toList()) + "`?").queue();
					return;
				} else if (!dk.getFields().contains(f)) {
					channel.sendMessage("❌ | Você não pode leiloar uma arena que não possui!").queue();
					return;
				}

				obj = f;
			}
		}

		try {
			int price = Integer.parseInt(args[2]);
			int min = switch (type) {
				case 1 -> ((KawaiponCard) obj).getCard().getRarity().getIndex() * (Constants.BASE_CARD_PRICE / 2) * (foil ? 2 : 1);
				case 2 -> Constants.BASE_EQUIPMENT_PRICE / 2;
				default -> Constants.BASE_FIELD_PRICE / 2;
			};

			if (price < min) {
				channel.sendMessage("❌ | Você não pode leiloar " + (type == 1 ? "essa carta" : type == 2 ? "esse equipamento" : "essa arena") + " por menos que " + StringHelper.separate(min) + " CR.").queue();
				return;
			}

			ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

			AtomicReference<Future<?>> event = new AtomicReference<>();

			AtomicInteger phase = new AtomicInteger(1);
			AtomicReference<Pair<User, Integer>> highest = new AtomicReference<>(null);

			SimpleMessageListener listener = new SimpleMessageListener(channel) {
				@Override
				public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent evt) {
					if (evt.getAuthor().isBot()) return;
					String raw = evt.getMessage().getContentRaw();
					if (StringUtils.isNumeric(raw)) {
						try {
							int offer = Integer.parseInt(raw);

							if (offer >= price && (highest.get() == null || offer > highest.get().getRight())) {
								Kawaipon offerer = KawaiponDAO.getKawaipon(evt.getAuthor().getId());
								Deck dk = offerer.getDeck();
								AtomicReference<Account> oacc = new AtomicReference<>(Account.find(Account.class, evt.getAuthor().getId()));

								switch (type) {
									case 1 -> {
										if (offerer.getCards().contains(obj) && !evt.getAuthor().getId().equals(author.getId())) {
											channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
											return;
										}
									}
									case 2 -> {
										if (dk.checkEquipment((Evogear) obj, channel)) return;
									}
									default -> {
										if (dk.checkField((Field) obj, channel)) return;
									}
								}

								if (oacc.get().getBalance() < offer) {
									channel.sendMessage("❌ | Você não possui CR suficientes!").queue();
									return;
								}

								highest.set(Pair.of(evt.getAuthor(), offer));
								phase.set(1);

								Main.getInfo().getConfirmationPending().put(author.getId() + "_L", true);
								channel.sendMessage(evt.getAuthor().getAsMention() + " ofereceu **" + StringHelper.separate(offer) + " CR**!").queue();

								event.get().cancel(true);
								event.set(exec.scheduleWithFixedDelay(() -> {
									if (phase.get() == 4 && highest.get() != null) {
										channel.sendMessage("**" + (type == 1 ? "Carta vendida" : type == 2 ? "Equipamento vendido" : "Arena vendida") + "** para " + highest.get().getLeft().getAsMention() + " por **" + StringHelper.separate(highest.get().getRight()) + "** CR!").queue();

										if (!author.getId().equals(highest.get().getLeft().getId())) {
											Kawaipon k = KawaiponDAO.getKawaipon(author.getId());
											Deck sdk = k.getDeck();

											Kawaipon buyer = KawaiponDAO.getKawaipon(highest.get().getLeft().getId());
											Deck bdk = buyer.getDeck();

											Account acc = Account.find(Account.class, author.getId());
											Account bacc = Account.find(Account.class, highest.get().getLeft().getId());

											acc.addCredit(highest.get().getRight(), AuctionCommand.class);
											bacc.removeCredit(highest.get().getRight(), AuctionCommand.class);

											switch (type) {
												case 1 -> {
													k.removeCard((KawaiponCard) obj);
													buyer.addCard((KawaiponCard) obj);
												}
												case 2 -> {
													sdk.removeEquipment((Evogear) obj);
													bdk.addEquipment((Evogear) obj);
												}
												default -> {
													sdk.removeField((Field) obj);
													bdk.addField((Field) obj);
												}
											}

											KawaiponDAO.saveKawaipon(k);
											KawaiponDAO.saveKawaipon(buyer);
											acc.save();
											bacc.save();
										}

										Main.getInfo().getConfirmationPending().remove(author.getId());
										close();
										event.get().cancel(true);
										exec.shutdownNow();
									} else {
										switch (phase.get()) {
											case 1 -> channel.sendMessage("Dou-lhe 1...").queue();
											case 2 -> channel.sendMessage("""
													Dou-lhe 2...
													Vamos lá pessoal, será que eu ouvi um %s?
													""".formatted(StringHelper.separate(highest.get().getRight() + 250))).queue();
											case 3 -> channel.sendMessage("Dou-lhe 3...").queue();
										}

										phase.getAndIncrement();
									}
								}, 5, 5, TimeUnit.SECONDS));
							}
						} catch (NumberFormatException e) {
							channel.sendMessage("❌ | O valor máximo é " + StringHelper.separate(Integer.MAX_VALUE) + " CR!").queue();
						}
					}
				}
			};

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Esta carta será vendida para quem oferecer o maior valor. Deseja mesmo leiloá-la?")
					.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
								if (wrapper.getUser().getId().equals(author.getId())) {
									Main.getInfo().getConfirmationPending().put(author.getId() + "_L", true);
									event.set(channel.sendMessage("Não houve nenhuma oferta, declaro o leilão **encerrado**!").queueAfter(30, TimeUnit.SECONDS, msg -> {
												Main.getInfo().getConfirmationPending().remove(author.getId() + "_L");
												listener.close();
											}
									));

									s.delete().mapToResult().flatMap(d -> channel.sendMessage("✅ | Leilão aberto com sucesso, se não houver ofertas maiores que **" + StringHelper.separate(price) + " CR** dentro de 30 segundos irei fechá-lo!")).queue();
									Main.getEvents().addHandler(guild, listener);
								}
							}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
					));
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O valor máximo é " + StringHelper.separate(Integer.MAX_VALUE) + " CR!").queue();
		}
	}
}
