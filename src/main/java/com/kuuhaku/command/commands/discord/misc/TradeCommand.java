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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.TradeContent;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Command(
		name = "trocar",
		aliases = {"trade"},
		usage = "req_mention",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EMBED_LINKS
})
public class TradeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(I18n.getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage("❌ | Você não pode trocar cartas com você mesmo.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(message.getMentionedUsers().get(0).getId()) != null) {
			channel.sendMessage("❌ | Este usuário possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
			return;
		}

		User tgt = message.getMentionedUsers().get(0);
		channel.sendMessage(tgt.getAsMention() + ", " + author.getAsMention() + " deseja negociar com você, deseja aceitar?").queue(s ->
				Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
							if (!mb.getId().equals(tgt.getId())) return;
							s.delete().queue(null, Helper::doNothing);
							Map<String, TradeContent> offers = Map.of(
									author.getId(), new TradeContent(author.getId()),
									tgt.getId(), new TradeContent(tgt.getId())
							);

							Account acc = AccountDAO.getAccount(author.getId());
							Account tacc = AccountDAO.getAccount(tgt.getId());

							EmbedBuilder eb = new ColorlessEmbedBuilder()
									.setTitle("Comércio entre " + author.getName() + " e " + tgt.getName())
									.setDescription("Para adicionar/remover uma oferta digite `+/- nome_da_carta [C]`, para definir uma quantia de créditos digite apenas `+ valor`.")
									.addField(author.getName() + " oferece:", offers.get(author.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(author.getId()).getValue()), true)
									.addField(tgt.getName() + " oferece:", offers.get(tgt.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(tgt.getId()).getValue()), true)
									.setFooter(author.getName() + ": " + Helper.separate(acc.getBalance()) + " CR\n" + tgt.getName() + ": " + Helper.separate(tacc.getBalance()) + " CR");

							sendTradeWindow(author, channel, guild, tgt, offers, acc, tacc, eb);
						}), true, 1, TimeUnit.MINUTES,
						u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
						ms -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());
							Main.getInfo().getConfirmationPending().remove(tgt.getId());
						}
				)
		);
	}

	private void sendTradeWindow(User author, TextChannel channel, Guild guild, User tgt, Map<String, TradeContent> offers, Account acc, Account tacc, EmbedBuilder eb) {
		channel.sendMessage(eb.build()).queue(msg -> {
			SimpleMessageListener sml = new SimpleMessageListener() {
				@Override
				public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
					User usr = event.getAuthor();
					String content = event.getMessage().getContentRaw();

					if (Helper.equalsAny(usr.getId(), author.getId(), tgt.getId()) && (content.startsWith("+") || content.startsWith("-"))) {
						boolean add = content.startsWith("+");
						String[] rawOffer = content.replaceFirst("[+\\-]", "").trim().split(" ");
						if (rawOffer.length < 1) return;

						boolean foil = Helper.equalsAny("c", rawOffer);
						String offer = rawOffer[0];
						TradeContent tc = offers.get(usr.getId());
						if (tc.isClosed()) {
							channel.sendMessage("❌ | Você já confirmou sua oferta.").queue();
							return;
						}

						if (StringUtils.isNumeric(offer)) {
							try {
								int c = Integer.parseInt(offer);
								if (c < 0) {
									channel.sendMessage("❌ | O valor deve ser maior ou igual a 0.").queue();
									return;
								} else if (tc.getAcc().getBalance() < c) {
									channel.sendMessage("❌ | Você não possui créditos suficientes.").queue();
									return;
								}

								tc.setCredits(c);
							} catch (NumberFormatException e) {
								channel.sendMessage("❌ | O valor máximo é " + Helper.separate(Integer.MAX_VALUE) + " créditos!").queue();
								return;
							}
						} else {
							CardType ct = CardDAO.identifyType(offer);
							switch (ct) {
								case KAWAIPON -> {
									KawaiponCard kc = new KawaiponCard(CardDAO.getCard(offer), foil);
									if (!tc.getKawaipon().getCards().contains(kc)) {
										channel.sendMessage("❌ | Você não possui essa carta.").queue();
										return;
									}

									if (add) {
										tc.getCards().add(kc);
										tc.getKp().removeCard(kc);
									} else {
										tc.getCards().remove(kc);
										tc.getKp().addCard(kc);
									}
								}
								case EVOGEAR -> {
									Equipment e = CardDAO.getEquipment(offer);
									if (!tc.getKawaipon().getEquipments().contains(e)) {
										channel.sendMessage("❌ | Você não possui essa carta.").queue();
										return;
									}

									if (add) {
										tc.getEquipments().add(e);
										tc.getKp().removeEquipment(e);
									} else {
										tc.getEquipments().remove(e);
										tc.getKp().addEquipment(e);
									}
								}
								case FIELD -> {
									Field f = CardDAO.getField(offer);
									if (!tc.getKawaipon().getFields().contains(f)) {
										channel.sendMessage("❌ | Você não possui essa carta.").queue();
										return;
									}

									if (add) {
										tc.getFields().add(f);
										tc.getKp().removeField(f);
									} else {
										tc.getFields().remove(f);
										tc.getKp().addField(f);
									}
								}
								case NONE -> {
									channel.sendMessage("❌ | Carta inexistente, você não quis dizer `" + Helper.didYouMean(offer, Stream.of(CardDAO.getAllCardNames(), CardDAO.getAllEquipmentNames(), CardDAO.getAllFieldNames()).flatMap(Collection::stream).toArray(String[]::new)) + "`?").queue();
									return;
								}
							}
						}

						for (TradeContent of : offers.values()) {
							of.setClosed(false);
						}
						eb.clearFields()
								.addField(author.getName() + " oferece:", offers.get(author.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(author.getId()).getValue()), true)
								.addField(tgt.getName() + " oferece:", offers.get(tgt.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(tgt.getId()).getValue()), true)
								.setFooter(author.getName() + ": " + Helper.separate(acc.getBalance()) + " CR\n" + tgt.getName() + ": " + Helper.separate(tacc.getBalance()) + " CR");

						msg.editMessage(eb.build()).queue(null, t -> {
							msg.delete().queue(null, Helper::doNothing);
							sendTradeWindow(author, channel, guild, tgt, offers, acc, tacc, eb);
						});

						event.getMessage().delete().queue(null, Helper::doNothing);
					}
				}
			};

			Pages.buttonize(msg, Collections.singletonMap(Helper.ACCEPT, (_mb, _ms) -> {
						offers.get(_mb.getId()).setClosed(true);

						if (offers.values().stream().allMatch(TradeContent::isClosed)) {
							int code = 0;
							User inv = null;
							for (TradeContent offer : offers.values()) {
								Account oAcc = offer.getAccount();
								Kawaipon oKp = offer.getKawaipon();

								code = oAcc.getBalance() >= offer.getCredits()
									   && oKp.getCards().containsAll(offer.getCards())
									   && oKp.getEquipments().containsAll(offer.getEquipments())
									   && oKp.getFields().containsAll(offer.getFields()) ? 0 : 1;

								if (code != 0) {
									inv = offer.getUid().equals(author.getId()) ? author : tgt;
									break;
								}

								TradeContent oT = offers.values().stream()
										.filter(t -> !t.getUid().equals(offer.getUid()))
										.findFirst().orElseThrow();

								if (offer.canReceive(oT.getKawaipon())) code = 0;
								else {
									inv = oT.getUid().equals(author.getId()) ? author : tgt;
									code = 2;
								}
							}

							if (code == 0) {
								if (offers.values().stream().mapToInt(t -> t.getCards().size() + t.getEquipments().size() + t.getFields().size()).sum() == 0) {
									channel.sendMessage("❌ | Transação inválida, você não pode realizar uma troca sem itens.").queue();
									for (TradeContent offer : offers.values()) {
										offer.setClosed(false);
									}
								} else if (TradeContent.isValidTrade(offers.values())) {
									msg.delete().queue(null, Helper::doNothing);
									sml.close();
									channel.sendMessage("Transação realizada com sucesso!").queue();
									TradeContent.trade(offers.values());
									return;
								} else {
									channel.sendMessage("❌ | Transação inválida, o valor entre as ofertas é injusto.").queue();
									for (TradeContent offer : offers.values()) {
										offer.setClosed(false);
									}
								}
							} else {
								switch (code) {
									case 1 -> channel.sendMessage("❌ | Transação inválida, " + inv.getAsMention() + " não possui todos os itens oferecidos ou não possui créditos suficientes.").queue();
									case 2 -> channel.sendMessage("❌ | Transação inválida, " + inv.getAsMention() + " possui um dos itens oferecidos.").queue();
								}
								for (TradeContent offer : offers.values()) {
									offer.setClosed(false);
								}
							}
						}

						eb.clearFields()
								.addField((offers.get(author.getId()).isClosed() ? "(CONFIRMADO) " : "") + author.getName() + " oferece:", offers.get(author.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(author.getId()).getValue()), true)
								.addField((offers.get(tgt.getId()).isClosed() ? "(CONFIRMADO) " : "") + tgt.getName() + " oferece:", offers.get(tgt.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(tgt.getId()).getValue()), true)
								.setFooter(author.getName() + ": " + Helper.separate(acc.getBalance()) + " CR\n" + tgt.getName() + ": " + Helper.separate(tacc.getBalance()) + " CR");

						msg.editMessage(eb.build()).queue(null, t -> {
							msg.delete().queue(null, Helper::doNothing);
							sendTradeWindow(author, channel, guild, tgt, offers, acc, tacc, eb);
						});
					}), true, 5, TimeUnit.MINUTES,
					u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
					_ms -> {
						msg.editMessage("Transação cancelada.")
								.flatMap(m -> m.suppressEmbeds(true))
								.queue(null, Helper::doNothing);
						sml.close();
						Main.getInfo().getConfirmationPending().remove(author.getId());
						Main.getInfo().getConfirmationPending().remove(tgt.getId());
					}
			);

			ShiroInfo.getShiroEvents().addHandler(guild, sml);
		});
	}
}
