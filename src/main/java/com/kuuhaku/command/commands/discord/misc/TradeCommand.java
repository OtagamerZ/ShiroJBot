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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
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
		name = "ttrocar",
		aliases = {"ttrade"},
		usage = "req_user",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class TradeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
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

							channel.sendMessage(eb.build()).queue(msg -> {
								SimpleMessageListener sml = new SimpleMessageListener() {
									@Override
									public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
										User usr = event.getAuthor();
										String content = event.getMessage().getContentRaw();

										if (Helper.equalsAny(usr.getId(), author.getId(), tgt.getId()) && (content.startsWith("+") || content.startsWith("-"))) {
											boolean add = content.startsWith("+");
											String offer = content.replaceFirst("\\+|-", "").trim();
											TradeContent tc = offers.get(usr.getId());
											if (tc.isClosed()) {
												channel.sendMessage("❌ | Você já confirmou sua oferta.").queue();
												return;
											}

											Account acc = AccountDAO.getAccount(usr.getId());

											if (StringUtils.isNumeric(offer)) {
												try {
													int c = Integer.parseInt(offer);
													if (acc.getBalance() < c) {
														channel.sendMessage("❌ | Você não possui créditos suficientes.").queue();
														return;
													}

													tc.setCredits(c);
												} catch (NumberFormatException e) {
													channel.sendMessage("❌ | O valor máximo é " + Helper.separate(Integer.MAX_VALUE) + " créditos!").queue();
													return;
												}
											} else {
												Kawaipon kp = KawaiponDAO.getKawaipon(usr.getId());
												CardType ct = CardDAO.identifyType(offer);
												switch (ct) {
													case KAWAIPON -> {
														KawaiponCard kc = new KawaiponCard(CardDAO.getCard(offer), StringUtils.containsIgnoreCase(offer, " c"));
														if (!kp.getCards().contains(kc)) {
															channel.sendMessage("❌ | Você não possui essa carta.").queue();
															return;
														}

														if (add) tc.getCards().add(kc);
														else tc.getCards().remove(kc);
													}
													case EVOGEAR -> {
														Equipment e = CardDAO.getEquipment(offer);
														if (!kp.getEquipments().contains(e)) {
															channel.sendMessage("❌ | Você não possui essa carta.").queue();
															return;
														}

														if (add) tc.getEquipments().add(e);
														else tc.getEquipments().remove(e);
													}
													case FIELD -> {
														Field f = CardDAO.getField(offer);
														if (!kp.getFields().contains(f)) {
															channel.sendMessage("❌ | Você não possui essa carta.").queue();
															return;
														}

														if (add) tc.getFields().add(f);
														else tc.getFields().remove(f);
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

											msg.editMessage(eb.build()).queue();

											event.getMessage().delete().queue(null, Helper::doNothing);
										}
									}
								};

								Pages.buttonize(msg, Collections.singletonMap(Helper.ACCEPT, (_mb, _ms) -> {
											if (offers.values().stream().allMatch(TradeContent::isClosed)) {
												boolean valid = true;
												User inv = null;
												for (TradeContent offer : offers.values()) {
													Account oAcc = AccountDAO.getAccount(offer.getUid());
													Kawaipon oKp = KawaiponDAO.getKawaipon(offer.getUid());

													valid = oAcc.getBalance() >= offer.getCredits()
															&& oKp.getCards().containsAll(offer.getCards())
															&& oKp.getEquipments().containsAll(offer.getEquipments())
															&& oKp.getFields().containsAll(offer.getFields());

													if (!valid) {
														inv = offer.getUid().equals(author.getId()) ? author : tgt;
														break;
													}
												}

												if (valid) {
													if (TradeContent.isValidTrade(offers.values())) {
														msg.delete().queue(null, Helper::doNothing);
														sml.close();
														channel.sendMessage("Transação realizada com sucesso!").queue();
														return;
													} else {
														channel.sendMessage("❌ | Transação inválida, o valor entre as ofertas é injusto.").queue();
														for (TradeContent offer : offers.values()) {
															offer.setClosed(false);
														}
													}
												} else {
													channel.sendMessage("❌ | Transação inválida, " + inv.getAsMention() + " não possui todos os itens oferecidos ou não possui créditos suficientes.").queue();
													for (TradeContent offer : offers.values()) {
														offer.setClosed(false);
													}
												}
											}

											eb.clearFields()
													.addField((offers.get(author.getId()).isClosed() ? "(CONFIRMADO) " : "") + author.getName() + " oferece:", offers.get(author.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(author.getId()).getValue()), true)
													.addField((offers.get(tgt.getId()).isClosed() ? "(CONFIRMADO) " : "") + tgt.getName() + " oferece:", offers.get(tgt.getId()).toString() + "\nValor base da oferta: " + Helper.separate(offers.get(tgt.getId()).getValue()), true)
													.setFooter(author.getName() + ": " + Helper.separate(acc.getBalance()) + " CR\n" + tgt.getName() + ": " + Helper.separate(tacc.getBalance()) + " CR");

											msg.editMessage(eb.build()).queue();
										}), true, 1, TimeUnit.MINUTES,
										u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
										_ms -> {
											msg.editMessage("Transação cancelada.").queue();
											sml.close();
											Main.getInfo().getConfirmationPending().remove(author.getId());
											Main.getInfo().getConfirmationPending().remove(tgt.getId());
										}
								);

								ShiroInfo.getShiroEvents().addHandler(guild, sml);
							});
						}), true, 1, TimeUnit.MINUTES,
						u -> Helper.equalsAny(u.getId(), author.getId(), tgt.getId()),
						ms -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());
							Main.getInfo().getConfirmationPending().remove(tgt.getId());
						}
				)
		);
	}
}
