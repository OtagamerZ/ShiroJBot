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
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "trocar",
		aliases = {"trade"},
		usage = "req_user-card-amount",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class TradeCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage("❌ | Você não pode trocar cartas com você mesmo.").queue();
			return;
		} else if (args.length < 4) {
			channel.sendMessage("❌ | Você precisa mencionar uma quantia de créditos ou uma carta, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada, `E` = evogear, `F` = campo) para realizar a troca.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(message.getMentionedUsers().get(0).getId()) != null) {
			channel.sendMessage("❌ | Este usuário possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
			return;
		}

		User other = message.getMentionedUsers().get(0);
		String text = StringUtils.normalizeSpace(String.join(" ", ArrayUtils.subarray(args, 1, args.length)));
		if (Helper.regex(text, "[\\w-]+ [NnCcEeFf] [\\w-]+ [NnCcEeFf]")) { //Trade
			if (args.length < 5) {
				channel.sendMessage("❌ | Você precisa mencionar uma carta, o tipo, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada, `E` = evogear, `F` = campo) para realizar a troca.").queue();
				return;
			}

			int[] types = {
					switch (args[2].toUpperCase(Locale.ROOT)) {
						case "N", "C" -> 1;
						case "E" -> 2;
						case "F" -> 3;
						default -> -1;
					},
					switch (args[4].toUpperCase(Locale.ROOT)) {
						case "N", "C" -> 1;
						case "E" -> 2;
						case "F" -> 3;
						default -> -1;
					}
			};

			if (types[0] == -1) {
				channel.sendMessage("❌ | Você precisa informar o tipo da carta que deseja oferecer (`N` = normal, `C` = cromada, `E` = evogear, `F` = campo).").queue();
				return;
			} else if (types[1] == -1) {
				channel.sendMessage("❌ | Você precisa informar o tipo da carta que deseja obter (`N` = normal, `C` = cromada, `E` = evogear, `F` = campo).").queue();
				return;
			}

			List<Pair<CardType, Object>> products = new ArrayList<>() {{
				add(switch (types[0]) {
					case 1 -> Pair.of(CardType.KAWAIPON, (Object) CardDAO.getCard(args[1], false));
					case 2 -> Pair.of(CardType.EVOGEAR, (Object) CardDAO.getEquipment(args[1]));
					default -> Pair.of(CardType.FIELD, (Object) CardDAO.getField(args[1]));
				});
				add(switch (types[1]) {
					case 1 -> Pair.of(CardType.KAWAIPON, (Object) CardDAO.getCard(args[3], false));
					case 2 -> Pair.of(CardType.EVOGEAR, (Object) CardDAO.getEquipment(args[3]));
					default -> Pair.of(CardType.FIELD, (Object) CardDAO.getField(args[3]));
				});
			}};

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());
			boolean yourFoil = args[2].equalsIgnoreCase("C");
			boolean hisFoil = args[4].equalsIgnoreCase("C");

			if (products.get(0).getRight() == null) {
				switch (types[0]) {
					case 1 -> {
						channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[1], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					case 2 -> {
						channel.sendMessage("❌ | Esse equipamento não existe, você não quis dizer `" + Helper.didYouMean(args[1], CardDAO.getAllEquipmentNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					default -> {
						channel.sendMessage("❌ | Essa arena não existe, você não quis dizer `" + Helper.didYouMean(args[1], CardDAO.getAllFieldNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
				}
			} else if (products.get(0).getRight() == null) {
				switch (types[1]) {
					case 1 -> {
						channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[3], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					case 2 -> {
						channel.sendMessage("❌ | Esse equipamento não existe, você não quis dizer `" + Helper.didYouMean(args[3], CardDAO.getAllEquipmentNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					default -> {
						channel.sendMessage("❌ | Essa arena não existe, você não quis dizer `" + Helper.didYouMean(args[3], CardDAO.getAllFieldNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
				}
			}

			if (types[0] == 1)
				products.set(0, Pair.of(products.get(0).getLeft(), new KawaiponCard((Card) products.get(0).getRight(), yourFoil)));
			if (types[1] == 1)
				products.set(1, Pair.of(products.get(1).getLeft(), new KawaiponCard((Card) products.get(1).getRight(), hisFoil)));

			if (AccountDAO.getAccount(kp.getUid()).getLoan() > 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-transfer-with-loan")).queue();
				return;
			} else if (AccountDAO.getAccount(target.getUid()).getLoan() > 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-with-loan")).queue();
				return;
			}

			switch (types[0]) {
				case 1 -> {
					KawaiponCard kc = (KawaiponCard) products.get(0).getRight();
					if (target.getCards().contains(kc)) {
						channel.sendMessage("❌ | Ele/ela já possui essa carta!").queue();
						return;
					} else if (!kp.getCards().contains(kc)) {
						channel.sendMessage("❌ | Parece que você não possui essa carta!").queue();
						return;
					}
				}
				case 2 -> {
					Equipment e = (Equipment) products.get(0).getRight();
					if (!kp.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Parece que você não possui esse equipamento!").queue();
						return;
					}

					if (target.checkEquipment(e, channel)) return;
				}
				default -> {
					Field f = (Field) products.get(0).getRight();
					if (!kp.getFields().contains(f)) {
						channel.sendMessage("❌ | Parece que você não possui essa arena!").queue();
						return;
					}

					if (target.checkField(f, channel)) return;
				}
			}

			switch (types[1]) {
				case 1 -> {
					KawaiponCard kc = (KawaiponCard) products.get(1).getRight();
					if (kp.getCards().contains(kc)) {
						channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
						return;
					} else if (!target.getCards().contains(kc)) {
						channel.sendMessage("❌ | Ele/ela não possui essa carta!").queue();
						return;
					}
				}
				case 2 -> {
					Equipment e = (Equipment) products.get(1).getRight();
					if (!target.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Ele/ela não possui esse equipamento!").queue();
						return;
					}

					if (kp.checkEquipment(e, channel)) return;
				}
				default -> {
					Field f = (Field) products.get(1).getRight();
					if (!target.getFields().contains(f)) {
						channel.sendMessage("❌ | Ele/ela não possui essa arena!").queue();
						return;
					}

					if (kp.checkField(f, channel)) return;
				}
			}

			String[] names = {
					switch (types[0]) {
						case 1 -> ((KawaiponCard) products.get(0).getRight()).getName();
						case 2 -> ((Equipment) products.get(0).getRight()).getCard().getName();
						default -> ((Field) products.get(0).getRight()).getCard().getName();
					},
					switch (types[1]) {
						case 1 -> ((KawaiponCard) products.get(1).getRight()).getName();
						case 2 -> ((Equipment) products.get(1).getRight()).getCard().getName();
						default -> ((Field) products.get(1).getRight()).getCard().getName();
					}
			};

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			Main.getInfo().getConfirmationPending().put(other.getId(), true);
			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja trocar a carta `" + names[0] + "` pela sua carta `" + names[1] + "`, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
								if (!mb.getId().equals(other.getId())) return;

								Main.getInfo().getConfirmationPending().remove(author.getId());
								Main.getInfo().getConfirmationPending().remove(other.getId());
								Kawaipon finalKp = KawaiponDAO.getKawaipon(author.getId());
								Kawaipon finalTarget = KawaiponDAO.getKawaipon(other.getId());

								switch (types[0]) {
									case 1 -> {
										KawaiponCard kc = (KawaiponCard) products.get(0).getRight();
										if (!finalKp.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui essa carta).")).queue(null, Helper::doNothing);
											return;
										} else if (finalTarget.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui essa carta).")).queue(null, Helper::doNothing);
											return;
										}

										finalKp.removeCard(kc);
										finalTarget.addCard(kc);
									}
									case 2 -> {
										Equipment eq = (Equipment) products.get(0).getRight();
										if (!finalKp.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalTarget.checkEquipmentError(eq)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
												return;
											}
											case 3 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui mais espaço para equipamentos).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalKp.removeEquipment(eq);
										finalTarget.addEquipment(eq);
									}
									default -> {
										Field f = (Field) products.get(0).getRight();
										if (!finalKp.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalTarget.checkFieldError(f)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui mais espaço para arenas).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalKp.removeField(f);
										finalTarget.addField(f);
									}
								}

								switch (types[1]) {
									case 1 -> {
										KawaiponCard kc = (KawaiponCard) products.get(1).getRight();
										if (!finalTarget.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui essa carta).")).queue(null, Helper::doNothing);
											return;
										} else if (finalKp.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui essa carta).")).queue(null, Helper::doNothing);
											return;
										}

										finalTarget.removeCard(kc);
										finalKp.addCard(kc);
									}
									case 2 -> {
										Equipment eq = (Equipment) products.get(1).getRight();
										if (!finalTarget.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalKp.checkEquipmentError(eq)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
												return;
											}
											case 3 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui mais espaço para equipamentos).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalTarget.removeEquipment(eq);
										finalKp.addEquipment(eq);
									}
									default -> {
										Field f = (Field) products.get(1).getRight();
										if (!finalTarget.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalKp.checkFieldError(f)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui mais espaço para arenas).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalTarget.removeField(f);
										finalKp.addField(f);
									}
								}

								KawaiponDAO.saveKawaipon(finalKp);
								KawaiponDAO.saveKawaipon(finalTarget);

								s.delete().flatMap(n -> channel.sendMessage("✅ | Troca concluída com sucesso!")).queue(null, Helper::doNothing);
							}), true, 1, TimeUnit.MINUTES,
							u -> Helper.equalsAny(u.getId(), author.getId(), other.getId()),
							ms -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());
								Main.getInfo().getConfirmationPending().remove(other.getId());
							}
					));
		} else if (Helper.regex(text, "(\\d+)[ ]+[\\w- ]+[NnCcEeFf]")) { //Purchase
			int type = switch (args[3].toUpperCase(Locale.ROOT)) {
				case "N", "C" -> 1;
				case "E" -> 2;
				case "F" -> 3;
				default -> -1;
			};

			if (type == -1) {
				channel.sendMessage("❌ | Tipo inválido, o tipo deve ser um dos seguntes valores: `N` = normal, `C` = cromada, `E` = evogear e `F` = campo.").queue();
				return;
			}

			int price = Integer.parseInt(args[1]);

			Pair<CardType, Object> product = switch (type) {
				case 1 -> Pair.of(CardType.KAWAIPON, CardDAO.getCard(args[2], false));
				case 2 -> Pair.of(CardType.EVOGEAR, CardDAO.getEquipment(args[2]));
				default -> Pair.of(CardType.FIELD, CardDAO.getField(args[2]));
			};

			boolean foil = args[3].equalsIgnoreCase("C");

			Account acc = AccountDAO.getAccount(author.getId());
			Account tacc = AccountDAO.getAccount(other.getId());

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());

			if (product.getRight() == null) {
				switch (type) {
					case 1 -> {
						channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[2], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					case 2 -> {
						channel.sendMessage("❌ | Esse equipamento não existe, você não quis dizer `" + Helper.didYouMean(args[2], CardDAO.getAllEquipmentNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					default -> {
						channel.sendMessage("❌ | Essa arena não existe, você não quis dizer `" + Helper.didYouMean(args[2], CardDAO.getAllFieldNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
				}
			}

			if (acc.getBalance() < price) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
				return;
			}

			if (type == 1)
				product = Pair.of(product.getLeft(), new KawaiponCard((Card) product.getRight(), foil));

			switch (type) {
				case 1 -> {
					KawaiponCard kc = (KawaiponCard) product.getRight();
					if (kp.getCards().contains(kc)) {
						channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
						return;
					} else if (!target.getCards().contains(kc)) {
						channel.sendMessage("❌ | Ele/ela não possui essa carta!").queue();
						return;
					}
				}
				case 2 -> {
					Equipment e = (Equipment) product.getRight();
					if (!target.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Ele/ela não possui esse equipamento!").queue();
						return;
					}

					if (kp.checkEquipment(e, channel)) return;
				}
				default -> {
					Field f = (Field) product.getRight();
					if (!target.getFields().contains(f)) {
						channel.sendMessage("❌ | Ele/ela não possui essa arena!").queue();
						return;
					}

					if (kp.checkField(f, channel)) return;
				}
			}

			boolean hasLoan = tacc.getLoan() > 0;
			int min = switch (type) {
				case 1 -> ((KawaiponCard) product.getRight())
								  .getCard()
								  .getRarity()
								  .getIndex() * (hasLoan ? Helper.BASE_CARD_PRICE * 2 : Helper.BASE_CARD_PRICE / 2) * (foil ? 2 : 1);
				case 2 -> ((Equipment) product.getRight())
								  .getTier() * (hasLoan ? Helper.BASE_EQUIPMENT_PRICE * 2 : Helper.BASE_EQUIPMENT_PRICE / 2);
				default -> hasLoan ? 20000 : 5000;
			};

			if (price < min) {
				if (hasLoan)
					channel.sendMessage("❌ | Como esse usuário possui uma dívida ativa, você não pode oferecer menos que " + min + " créditos por " + (type == 1 ? "essa carta" : type == 2 ? "esse equipamento" : "essa arena") + ".").queue();
				else
					channel.sendMessage("❌ | Você não pode oferecer menos que " + min + " créditos por " + (type == 1 ? "essa carta" : type == 2 ? "esse equipamento" : "essa arena") + ".").queue();
				return;
			}

			String name = switch (type) {
				case 1 -> ((KawaiponCard) product.getRight()).getName();
				case 2 -> ((Equipment) product.getRight()).getCard().getName();
				default -> ((Field) product.getRight()).getCard().getName();
			};

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			Main.getInfo().getConfirmationPending().put(other.getId(), true);
			Pair<CardType, Object> finalProduct = product;
			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja comprar " + (type == 1 ? "sua carta" : type == 2 ? "seu equipamento" : "sua arena") + " `" + name + "` por " + price + " créditos, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
								if (!mb.getId().equals(other.getId())) return;

								Main.getInfo().getConfirmationPending().remove(author.getId());
								Main.getInfo().getConfirmationPending().remove(other.getId());
								Kawaipon finalKp = KawaiponDAO.getKawaipon(author.getId());
								Kawaipon finalTarget = KawaiponDAO.getKawaipon(other.getId());

								switch (type) {
									case 1 -> {
										KawaiponCard kc = (KawaiponCard) finalProduct.getRight();
										if (!finalTarget.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui essa carta).")).queue(null, Helper::doNothing);
											return;
										} else if (finalKp.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui essa carta).")).queue(null, Helper::doNothing);
											return;
										}

										finalTarget.removeCard(kc);
										finalKp.addCard(kc);
										CardMarketDAO.saveCard(new CardMarket(other.getId(), author.getId(), kc, price));
									}
									case 2 -> {
										Equipment eq = (Equipment) finalProduct.getRight();
										if (!finalTarget.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalKp.checkEquipmentError(eq)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
												return;
											}
											case 3 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui mais espaço para equipamentos).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalTarget.removeEquipment(eq);
										finalKp.addEquipment(eq);
										EquipmentMarketDAO.saveCard(new EquipmentMarket(other.getId(), author.getId(), eq, price));
									}
									default -> {
										Field f = (Field) finalProduct.getRight();
										if (!finalTarget.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalKp.checkFieldError(f)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui mais espaço para arenas).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalTarget.removeField(f);
										finalKp.addField(f);
										FieldMarketDAO.saveCard(new FieldMarket(other.getId(), author.getId(), f, price));
									}
								}

								int liquidAmount = Helper.applyTax(tacc.getUid(), price, 0.1);
								boolean taxed = price != liquidAmount;

								acc.removeCredit(price, this.getClass());
								tacc.addCredit(liquidAmount, this.getClass());

								LotteryValue lv = LotteryDAO.getLotteryValue();
								lv.addValue(price - liquidAmount);
								LotteryDAO.saveLotteryValue(lv);

								KawaiponDAO.saveKawaipon(finalKp);
								KawaiponDAO.saveKawaipon(finalTarget);
								AccountDAO.saveAccount(acc);
								AccountDAO.saveAccount(tacc);

								if (taxed) {
									s.delete().flatMap(n -> channel.sendMessage("✅ | Venda concluída com sucesso! (taxa de venda: " + Helper.roundToString(Helper.prcnt(price, liquidAmount) * 100 - 100, 1) + "%)")).queue(null, Helper::doNothing);
								} else {
									s.delete().flatMap(n -> channel.sendMessage("✅ | Venda concluída com sucesso! (Exceed vitorioso isento de taxa)")).queue(null, Helper::doNothing);
								}
							}), true, 1, TimeUnit.MINUTES,
							u -> Helper.equalsAny(u.getId(), author.getId(), other.getId()),
							ms -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());
								Main.getInfo().getConfirmationPending().remove(other.getId());
							}
					));
		} else if (Helper.regex(text, "[\\w- ]+[NnCcEeFf][ ]+(\\d+)")) { //Selling
			int type = switch (args[2].toUpperCase(Locale.ROOT)) {
				case "N", "C" -> 1;
				case "E" -> 2;
				case "F" -> 3;
				default -> -1;
			};

			if (type == -1) {
				channel.sendMessage("❌ | Tipo inválido, o tipo deve ser um dos seguntes valores: `N` = normal, `C` = cromada, `E` = evogear e `F` = campo.").queue();
				return;
			}

			int price = Integer.parseInt(args[3]);

			Pair<CardType, Object> product = switch (type) {
				case 1 -> Pair.of(CardType.KAWAIPON, CardDAO.getCard(args[1], false));
				case 2 -> Pair.of(CardType.EVOGEAR, CardDAO.getEquipment(args[1]));
				default -> Pair.of(CardType.FIELD, CardDAO.getField(args[1]));
			};

			boolean foil = args[2].equalsIgnoreCase("C");

			Account acc = AccountDAO.getAccount(author.getId());
			Account tacc = AccountDAO.getAccount(other.getId());

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());

			if (product.getRight() == null) {
				switch (type) {
					case 1 -> {
						channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[2], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					case 2 -> {
						channel.sendMessage("❌ | Esse equipamento não existe, você não quis dizer `" + Helper.didYouMean(args[2], CardDAO.getAllEquipmentNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
					default -> {
						channel.sendMessage("❌ | Essa arena não existe, você não quis dizer `" + Helper.didYouMean(args[2], CardDAO.getAllFieldNames().toArray(String[]::new)) + "`?").queue();
						return;
					}
				}
			}

			if (tacc.getBalance() < price) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-target")).queue();
				return;
			}

			if (type == 1)
				product = Pair.of(product.getLeft(), new KawaiponCard((Card) product.getRight(), foil));

			switch (type) {
				case 1 -> {
					KawaiponCard kc = (KawaiponCard) product.getRight();
					if (target.getCards().contains(kc)) {
						channel.sendMessage("❌ | Ele/ela já possui essa carta!").queue();
						return;
					} else if (!kp.getCards().contains(kc)) {
						channel.sendMessage("❌ | Parece que você não possui essa carta!").queue();
						return;
					}
				}
				case 2 -> {
					Equipment e = (Equipment) product.getRight();
					if (!kp.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Parece que você não possui esse equipamento!").queue();
						return;
					}

					if (target.checkEquipment(e, channel)) return;
				}
				default -> {
					Field f = (Field) product.getRight();
					if (!kp.getFields().contains(f)) {
						channel.sendMessage("❌ | Parece que você não possui essa arena!").queue();
						return;
					}

					if (target.checkField(f, channel)) return;
				}
			}

			boolean hasLoan = acc.getLoan() > 0;
			int min = switch (type) {
				case 1 -> ((KawaiponCard) product.getRight())
								  .getCard()
								  .getRarity()
								  .getIndex() * (hasLoan ? Helper.BASE_CARD_PRICE * 2 : Helper.BASE_CARD_PRICE / 2) * (foil ? 2 : 1);
				case 2 -> ((Equipment) product.getRight())
								  .getTier() * (hasLoan ? Helper.BASE_EQUIPMENT_PRICE * 2 : Helper.BASE_EQUIPMENT_PRICE / 2);
				default -> hasLoan ? 20000 : 5000;
			};

			if (price < min) {
				if (hasLoan)
					channel.sendMessage("❌ | Como você possui uma dívida ativa, você não pode cobrar menos que " + min + " créditos por " + (type == 1 ? "essa carta" : type == 2 ? "esse equipamento" : "essa arena") + ".").queue();
				else
					channel.sendMessage("❌ | Você não pode cobrar menos que " + min + " créditos por " + (type == 1 ? "essa carta" : type == 2 ? "esse equipamento" : "essa arena") + ".").queue();
				return;
			}

			String name = switch (type) {
				case 1 -> ((KawaiponCard) product.getRight()).getName();
				case 2 -> ((Equipment) product.getRight()).getCard().getName();
				default -> ((Field) product.getRight()).getCard().getName();
			};

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			Main.getInfo().getConfirmationPending().put(other.getId(), true);
			Pair<CardType, Object> finalProduct = product;
			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja vender " + (type == 1 ? "a carta" : type == 2 ? "o equipamento" : "a arena") + " `" + name + "` por " + price + " créditos, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
								if (!mb.getId().equals(other.getId())) return;

								Main.getInfo().getConfirmationPending().remove(author.getId());
								Main.getInfo().getConfirmationPending().remove(other.getId());
								Kawaipon finalKp = KawaiponDAO.getKawaipon(author.getId());
								Kawaipon finalTarget = KawaiponDAO.getKawaipon(other.getId());

								switch (type) {
									case 1 -> {
										KawaiponCard kc = (KawaiponCard) finalProduct.getRight();
										if (!finalKp.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui essa carta).")).queue(null, Helper::doNothing);
											return;
										} else if (finalTarget.getCards().contains(kc)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui essa carta).")).queue(null, Helper::doNothing);
											return;
										}

										finalKp.removeCard(kc);
										finalTarget.addCard(kc);
										CardMarketDAO.saveCard(new CardMarket(author.getId(), other.getId(), kc, price));
									}
									case 2 -> {
										Equipment eq = (Equipment) finalProduct.getRight();
										if (!finalKp.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalTarget.checkEquipmentError(eq)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
												return;
											}
											case 3 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui mais espaço para equipamentos).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalKp.removeEquipment(eq);
										finalTarget.addEquipment(eq);
										EquipmentMarketDAO.saveCard(new EquipmentMarket(author.getId(), other.getId(), eq, price));
									}
									default -> {
										Field f = (Field) finalProduct.getRight();
										if (!finalKp.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										}

										switch (finalTarget.checkFieldError(f)) {
											case 1 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
												return;
											}
											case 2 -> {
												s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui mais espaço para arenas).")).queue(null, Helper::doNothing);
												return;
											}
										}

										finalKp.removeField(f);
										finalTarget.addField(f);
										FieldMarketDAO.saveCard(new FieldMarket(author.getId(), other.getId(), f, price));
									}
								}

								int liquidAmount = Helper.applyTax(acc.getUid(), price, 0.1);
								boolean taxed = price != liquidAmount;

								tacc.removeCredit(price, this.getClass());
								acc.addCredit(liquidAmount, this.getClass());

								LotteryValue lv = LotteryDAO.getLotteryValue();
								lv.addValue(price - liquidAmount);
								LotteryDAO.saveLotteryValue(lv);

								KawaiponDAO.saveKawaipon(finalKp);
								KawaiponDAO.saveKawaipon(finalTarget);
								AccountDAO.saveAccount(acc);
								AccountDAO.saveAccount(tacc);

								if (taxed) {
									s.delete().flatMap(n -> channel.sendMessage("✅ | Venda concluída com sucesso! (taxa de venda: " + Helper.roundToString(Helper.prcnt(price, liquidAmount) * 100 - 100, 1) + "%)")).queue(null, Helper::doNothing);
								} else {
									s.delete().flatMap(n -> channel.sendMessage("✅ | Venda concluída com sucesso! (Exceed vitorioso isento de taxa)")).queue(null, Helper::doNothing);
								}
							}), true, 1, TimeUnit.MINUTES,
							u -> Helper.equalsAny(u.getId(), author.getId(), other.getId()),
							ms -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());
								Main.getInfo().getConfirmationPending().remove(other.getId());
							}
					));
		}
	}
}
