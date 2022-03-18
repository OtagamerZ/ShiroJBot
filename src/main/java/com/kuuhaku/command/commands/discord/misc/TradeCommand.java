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
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.TradeDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Command(
		name = "trocar",
		aliases = {"trade"},
		usage = "req_mention",
		category = Category.SUPPORT
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EMBED_LINKS
})
public class TradeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(I18n.getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage("❌ | Você não pode trocar cartas com você mesmo.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(message.getMentionedUsers().get(0).getId()) != null) {
			channel.sendMessage("❌ | Este usuário possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
			return;
		}

		User tgt = message.getMentionedUsers().get(0);
		Trade t = TradeDAO.getTrade(author.getId(), tgt.getId());
		if (t != null) {
			String other = t.getLeft().getUid().equals(author.getId()) ? t.getRight().getUid() : t.getLeft().getUid();
			String name = Helper.getUsername(other);
			if (!tgt.getId().equals(other)) {
				channel.sendMessage("❌ | Você possui uma troca pendente com " + Helper.unmention(name) + ".").queue();
				return;
			}
		}

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage(tgt.getAsMention() + ", " + author.getAsMention() + " deseja" + (t == null ? "" : " continuar a") + " negociar com você, deseja aceitar?").queue(s ->
				Pages.buttonize(s, Collections.singletonMap(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
							if (!wrapper.getUser().getId().equals(tgt.getId())) return;
							s.delete().queue(null, Helper::doNothing);
							Trade trade = Helper.getOr(t, new Trade(author.getId(), tgt.getId()));

							EmbedBuilder eb = new ColorlessEmbedBuilder()
									.setTitle("Comércio entre " + author.getName() + " e " + tgt.getName())
									.setDescription("Para adicionar/remover uma oferta digite `+/- nome_da_carta`, para adicionar/remover uma quantia de CR digite `+/- valor`.")
									.addField(author.getName() + " oferece:", trade.getLeft() + "\nValor base da oferta: " + Helper.separate(trade.getLeft().getValue()), true)
									.addField(tgt.getName() + " oferece:", trade.getRight() + "\nValor base da oferta: " + Helper.separate(trade.getRight().getValue()), true)
									.setFooter("%s: %s CR\n%s: %s CR".formatted(
											author.getName(),
											Helper.separate(trade.getLeft().getAccount().getBalance()),
											tgt.getName(),
											Helper.separate(trade.getRight().getAccount().getBalance())
									));

							sendTradeWindow(author, channel, guild, tgt, trade, eb);
						}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
						u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				)
		);
	}

	private void sendTradeWindow(User author, TextChannel channel, Guild guild, User tgt, Trade trade, EmbedBuilder eb) {
		Main.getInfo().getConfirmationPending().put(author.getId() + "_T", true);
		Main.getInfo().getConfirmationPending().put(tgt.getId() + "_T", true);
		channel.sendMessageEmbeds(eb.build()).queue(msg -> {
			SimpleMessageListener sml = new SimpleMessageListener() {
				@Override
				public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
					User usr = event.getAuthor();
					String content = event.getMessage().getContentRaw();

					if (Helper.equalsAny(usr.getId(), author.getId(), tgt.getId()) && (content.startsWith("+") || content.startsWith("-"))) {
						boolean add = content.startsWith("+");
						String[] rawOffer = content.replaceFirst("[+\\-]", "").trim().split(" ");
						if (rawOffer.length < 1) return;

						String offer = rawOffer[0];

						TradeOffer to = trade.getOffer(usr.getId());
						if (to.hasAccepted()) {
							channel.sendMessage("❌ | Você já confirmou sua oferta.").queue();
							return;
						}

						if (StringUtils.isNumeric(offer)) {
							try {
								int c = Integer.parseInt(offer);
								if (c < 0) {
									channel.sendMessage("❌ | O valor deve ser maior ou igual a 0.").queue();
									return;
								}

								if (add) {
									if (to.getAccount().getBalance() < to.getValue() + c) {
										channel.sendMessage("❌ | Você não possui CR suficientes.").queue();
										return;
									}

									to.addValue(c);

									Account acc = to.getAccount();
									acc.removeCredit(c, this.getClass());
									AccountDAO.saveAccount(acc);
								} else {
									if (to.getValue() - c < 0) {
										channel.sendMessage("❌ | Você não pode reduzir o CR oferecido para menos que 0.").queue();
										return;
									}

									to.removeValue(c);

									Account acc = to.getAccount();
									acc.addCredit(c, this.getClass());
									AccountDAO.saveAccount(acc);
								}
							} catch (NumberFormatException e) {
								channel.sendMessage("❌ | O valor máximo é " + Helper.separate(Integer.MAX_VALUE) + " CR!").queue();
								return;
							}
						} else {
							if (add) {
								Kawaipon kp = to.getKawaipon();
								Deck dk = kp.getDeck();

								String name = offer.toUpperCase(Locale.ROOT);
								EnumSet<CardType> matches = EnumSet.noneOf(CardType.class);
								kp.getCards().stream()
										.filter(kc -> kc.getCard().getId().equals(name))
										.findFirst()
										.ifPresent(kc -> matches.add(CardType.KAWAIPON));
								dk.getChampions().stream()
										.filter(kc -> kc.getCard().getId().equals(name))
										.findFirst()
										.ifPresent(kc -> matches.add(CardType.SENSHI));
								dk.getEquipments().stream()
										.filter(e -> e.getCard().getId().equals(name))
										.findFirst()
										.ifPresent(e -> matches.add(CardType.EVOGEAR));
								dk.getFields().stream()
										.filter(f -> f.getCard().getId().equals(name))
										.findFirst()
										.ifPresent(f -> matches.add(CardType.FIELD));

								CompletableFuture<Triple<Card, CardType, Boolean>> chosen = new CompletableFuture<>();
								if (matches.size() > 1) {
									EmbedBuilder eb = new ColorlessEmbedBuilder()
											.setTitle("Por favor escolha uma")
											.setDescription(
													(matches.contains(CardType.KAWAIPON) ? ":regional_indicator_k: -> Kawaipon\n" : "") +
															(matches.contains(CardType.SENSHI) ? ":regional_indicator_c: -> Campeão\n" : "") +
															(matches.contains(CardType.EVOGEAR) ? ":regional_indicator_e: -> Evogear\n" : "") +
															(matches.contains(CardType.FIELD) ? ":regional_indicator_f: -> Campo\n" : "")
											);

									Map<Emoji, ThrowingConsumer<ButtonWrapper>> btns = new LinkedHashMap<>();
									if (matches.contains(CardType.KAWAIPON)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDF0"), wrapper -> {
											chooseVersion(author, channel, kp, name, chosen);
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}
									if (matches.contains(CardType.SENSHI)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDE8"), wrapper -> {
											chosen.complete(Triple.of(CardDAO.getRawCard(name), CardType.SENSHI, false));
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}
									if (matches.contains(CardType.EVOGEAR)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDEA"), wrapper -> {
											chosen.complete(Triple.of(CardDAO.getRawCard(name), CardType.EVOGEAR, false));
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}
									if (matches.contains(CardType.FIELD)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDEB"), wrapper -> {
											chosen.complete(Triple.of(CardDAO.getRawCard(name), CardType.FIELD, false));
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}

									Main.getInfo().getConfirmationPending().put(author.getId() + "_T", true);
									Main.getInfo().getConfirmationPending().put(tgt.getId() + "_T", true);
									channel.sendMessageEmbeds(eb.build())
											.queue(s -> Pages.buttonize(s, btns, true,
													ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES,
													u -> u.getId().equals(author.getId()),
													ms -> {
														Main.getInfo().getConfirmationPending().remove(author.getId() + "_T");
														Main.getInfo().getConfirmationPending().remove(tgt.getId() + "_T");
														chosen.complete(null);
													}
											));
								} else if (matches.isEmpty()) {
									channel.sendMessage("❌ | Você não pode oferecer uma carta que não possui!").queue();
									return;
								} else {
									CardType type = matches.stream().findFirst().orElse(CardType.NONE);
									switch (type) {
										case KAWAIPON -> chooseVersion(author, channel, kp, name, chosen);
										case SENSHI, EVOGEAR, FIELD -> chosen.complete(Triple.of(CardDAO.getRawCard(name), type, false));
										case NONE -> chosen.complete(null);
									}
								}

								try {
									Triple<Card, CardType, Boolean> off = chosen.get();
									if (off == null) {
										channel.sendMessage("Seleção cancelada.").queue();
										return;
									}

									Kawaipon finalKp = to.getKawaipon();
									Deck fDk = finalKp.getDeck();

									if (fDk.isNovice() && off.getMiddle() == CardType.SENSHI) {
										channel.sendMessage("❌ | Você não pode fazer esta operação com o deck de iniciante!").queue();
										return;
									}

									TradeCard tc = switch (off.getMiddle()) {
										case EVOGEAR -> {
											Equipment e = fDk.getEquipment(off.getLeft());
											fDk.removeEquipment(e);
											if (e == null) yield null;

											yield new TradeCard(off.getLeft(), off.getMiddle());
										}
										case FIELD -> {
											Field f = fDk.getField(off.getLeft());
											fDk.removeField(f);
											if (f == null) yield null;

											yield new TradeCard(off.getLeft(), off.getMiddle());
										}
										case SENSHI -> {
											Champion c = fDk.getChampion(off.getLeft());
											fDk.removeChampion(c);
											if (c == null) yield null;

											yield new TradeCard(off.getLeft(), off.getMiddle());
										}
										default -> {
											KawaiponCard kc = finalKp.getCard(off.getLeft(), off.getRight());
											finalKp.removeCard(kc);
											if (kc == null) yield null;

											yield new TradeCard(off.getLeft(), off.getMiddle(), off.getRight());
										}
									};
									if (tc == null) {
										channel.sendMessage("❌ | Você não pode oferecer uma carta que não possui!").queue();
										return;
									}

									to.getCards().add(tc);

									KawaiponDAO.saveKawaipon(finalKp);
								} catch (InterruptedException | ExecutionException e) {
									Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
								}
							} else {
								String name = offer.toUpperCase(Locale.ROOT);
								EnumSet<CardType> matches = EnumSet.noneOf(CardType.class);
								to.getCards().stream()
										.filter(c -> c.getCard().getId().equals(name))
										.map(TradeCard::getType)
										.forEach(matches::add);

								CompletableFuture<Triple<Card, CardType, Boolean>> chosen = new CompletableFuture<>();
								if (matches.size() > 1) {
									EmbedBuilder eb = new ColorlessEmbedBuilder()
											.setTitle("Por favor escolha uma")
											.setDescription(
													(matches.contains(CardType.KAWAIPON) ? ":regional_indicator_k: -> Kawaipon\n" : "") +
															(matches.contains(CardType.SENSHI) ? ":regional_indicator_c: -> Campeão\n" : "") +
															(matches.contains(CardType.EVOGEAR) ? ":regional_indicator_e: -> Evogear\n" : "") +
															(matches.contains(CardType.FIELD) ? ":regional_indicator_f: -> Campo\n" : "")
											);

									Map<Emoji, ThrowingConsumer<ButtonWrapper>> btns = new LinkedHashMap<>();
									if (matches.contains(CardType.KAWAIPON)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDF0"), wrapper -> {
											chooseVersion(author, channel, to, name, chosen);
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}
									if (matches.contains(CardType.SENSHI)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDE8"), wrapper -> {
											chosen.complete(Triple.of(CardDAO.getRawCard(name), CardType.SENSHI, false));
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}
									if (matches.contains(CardType.EVOGEAR)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDEA"), wrapper -> {
											chosen.complete(Triple.of(CardDAO.getRawCard(name), CardType.EVOGEAR, false));
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}
									if (matches.contains(CardType.FIELD)) {
										btns.put(Helper.parseEmoji("\uD83C\uDDEB"), wrapper -> {
											chosen.complete(Triple.of(CardDAO.getRawCard(name), CardType.FIELD, false));
											wrapper.getMessage().delete().queue(null, Helper::doNothing);
										});
									}

									Main.getInfo().getConfirmationPending().put(author.getId() + "_T", true);
									Main.getInfo().getConfirmationPending().put(tgt.getId() + "_T", true);
									channel.sendMessageEmbeds(eb.build())
											.queue(s -> Pages.buttonize(s, btns, true,
													ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES,
													u -> u.getId().equals(author.getId()),
													ms -> {
														Main.getInfo().getConfirmationPending().remove(author.getId() + "_T");
														Main.getInfo().getConfirmationPending().remove(tgt.getId() + "_T");
														chosen.complete(null);
													}
											));
								} else if (matches.isEmpty()) {
									channel.sendMessage("❌ | Você não pode retornar uma carta que não ofereceu!").queue();
									return;
								} else {
									CardType type = matches.stream().findFirst().orElse(CardType.NONE);
									switch (type) {
										case KAWAIPON -> chooseVersion(author, channel, to, name, chosen);
										case SENSHI, EVOGEAR, FIELD -> chosen.complete(Triple.of(CardDAO.getRawCard(name), type, false));
										case NONE -> chosen.complete(null);
									}
								}

								try {
									Triple<Card, CardType, Boolean> off = chosen.get();
									if (off == null) {
										channel.sendMessage("Seleção cancelada.").queue();
										return;
									}

									TradeCard card = to.getCards().stream()
											.filter(c ->
													c.getCard().equals(off.getLeft())
															&& c.getType() == off.getMiddle()
															&& c.isFoil() == off.getRight()
											)
											.findFirst()
											.orElse(null);
									if (card == null) {
										channel.sendMessage("❌ | Você não pode retornar uma carta que não ofereceu!").queue();
										return;
									}

									to.rollback(card);
								} catch (InterruptedException | ExecutionException e) {
									Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
								}
							}
						}

						for (TradeOffer of : trade.getOffers()) {
							of.setAccepted(false);
						}
						TradeDAO.saveTrade(trade);

						eb.clearFields()
								.addField(author.getName() + " oferece:", trade.getLeft() + "\nValor base da oferta: " + Helper.separate(trade.getLeft().getValue()), true)
								.addField(tgt.getName() + " oferece:", trade.getRight() + "\nValor base da oferta: " + Helper.separate(trade.getRight().getValue()), true)
								.setFooter("%s: %s CR\n%s: %s CR".formatted(
										author.getName(),
										Helper.separate(trade.getLeft().getAccount().getBalance()),
										tgt.getName(),
										Helper.separate(trade.getRight().getAccount().getBalance())
								));

						msg.editMessageEmbeds(eb.build()).queue(null, t -> {
							msg.delete().queue(null, Helper::doNothing);
							sendTradeWindow(author, channel, guild, tgt, trade, eb);
						});

						event.getMessage().delete().queue(null, Helper::doNothing);
					}
				}
			};

			Pages.buttonize(msg, Collections.singletonMap(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
						if (trade.getOffers().stream().anyMatch(o -> o.getUid().equals(wrapper.getUser().getId()))) {
							trade.getOffer(wrapper.getUser().getId()).setAccepted(true);

							if (trade.getOffers().stream().allMatch(TradeOffer::hasAccepted)) {
								Main.getInfo().getConfirmationPending().remove(author.getId() + "_T");
								Main.getInfo().getConfirmationPending().remove(tgt.getId() + "_T");

								trade.getLeft().commit(trade.getRight().getUid());
								trade.getRight().commit(trade.getLeft().getUid());
								trade.setFinished(true);

								msg.delete().queue(null, Helper::doNothing);
								sml.close();
								channel.sendMessage("Transação realizada com sucesso!").queue();
								TradeDAO.saveTrade(trade);
								return;
							}

							eb.clearFields()
									.addField((trade.getLeft().hasAccepted() ? "(CONFIRMADO) " : "") + author.getName() + " oferece:", trade.getLeft() + "\nValor base da oferta: " + Helper.separate(trade.getLeft().getValue()), true)
									.addField((trade.getRight().hasAccepted() ? "(CONFIRMADO) " : "") + tgt.getName() + " oferece:", trade.getRight() + "\nValor base da oferta: " + Helper.separate(trade.getRight().getValue()), true)
									.setFooter("%s: %s CR\n%s: %s CR".formatted(
											author.getName(),
											Helper.separate(trade.getLeft().getAccount().getBalance()),
											tgt.getName(),
											Helper.separate(trade.getRight().getAccount().getBalance())
									));

							msg.editMessageEmbeds(eb.build()).queue(null, t -> {
								msg.delete().queue(null, Helper::doNothing);
								sendTradeWindow(author, channel, guild, tgt, trade, eb);
							});
						}
					}), ShiroInfo.USE_BUTTONS, true, 5, TimeUnit.MINUTES,
					u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
					_ms -> {
						Main.getInfo().getConfirmationPending().remove(author.getId() + "_T");
						Main.getInfo().getConfirmationPending().remove(tgt.getId() + "_T");
						msg.editMessage("Transação cancelada.")
								.flatMap(m -> m.suppressEmbeds(true))
								.queue(null, Helper::doNothing);
						sml.close();

						for (TradeOffer offer : trade.getOffers()) {
							offer.rollback();
						}

						TradeDAO.removeTrade(trade);
					}
			);

			ShiroInfo.getShiroEvents().addHandler(guild, sml);
		});
	}

	private void chooseVersion(User author, TextChannel channel, Kawaipon kp, String name, CompletableFuture<Triple<Card, CardType, Boolean>> chosen) {
		List<KawaiponCard> kcs = kp.getCards().stream()
				.filter(kc -> kc.getCard().getId().equals(name))
				.sorted(Comparator.comparing(KawaiponCard::isFoil))
				.toList();

		if (kcs.size() > 1) {
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Foram encontradas 2 versões dessa carta (normal e cromada). Por favor selecione **:one: para normal** ou **:two: para cromada**.")
					.queue(s -> Pages.buttonize(s, new LinkedHashMap<>() {{
								put(Helper.parseEmoji(Helper.getNumericEmoji(1)), wrapper -> {
									chosen.complete(Triple.of(kcs.get(0).getCard(), CardType.KAWAIPON, false));
									wrapper.getMessage().delete().queue(null, Helper::doNothing);
								});
								put(Helper.parseEmoji(Helper.getNumericEmoji(2)), wrapper -> {
									chosen.complete(Triple.of(kcs.get(1).getCard(), CardType.KAWAIPON, true));
									wrapper.getMessage().delete().queue(null, Helper::doNothing);
								});
							}}, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());
								chosen.complete(null);
							}
					));
		} else {
			chosen.complete(Triple.of(kcs.get(0).getCard(), CardType.KAWAIPON, kcs.get(0).isFoil()));
		}
	}

	private void chooseVersion(User author, TextChannel channel, TradeOffer to, String name, CompletableFuture<Triple<Card, CardType, Boolean>> chosen) {
		List<TradeCard> tcs = to.getCards().stream()
				.filter(kc -> kc.getCard().getId().equals(name))
				.sorted(Comparator.comparing(TradeCard::isFoil))
				.toList();

		if (tcs.size() > 1) {
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Foram encontradas 2 versões dessa carta (normal e cromada). Por favor selecione **:one: para normal** ou **:two: para cromada**.")
					.queue(s -> Pages.buttonize(s, new LinkedHashMap<>() {{
								put(Helper.parseEmoji(Helper.getNumericEmoji(1)), wrapper -> {
									chosen.complete(Triple.of(tcs.get(0).getCard(), CardType.KAWAIPON, false));
									wrapper.getMessage().delete().queue(null, Helper::doNothing);
								});
								put(Helper.parseEmoji(Helper.getNumericEmoji(2)), wrapper -> {
									chosen.complete(Triple.of(tcs.get(1).getCard(), CardType.KAWAIPON, true));
									wrapper.getMessage().delete().queue(null, Helper::doNothing);
								});
							}}, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());
								chosen.complete(null);
							}
					));
		} else {
			chosen.complete(Triple.of(tcs.get(0).getCard(), CardType.KAWAIPON, tcs.get(0).isFoil()));
		}
	}
}
