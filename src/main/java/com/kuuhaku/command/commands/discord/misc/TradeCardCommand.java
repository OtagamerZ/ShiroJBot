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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TradeCardCommand extends Command {

	public TradeCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public TradeCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public TradeCardCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public TradeCardCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		} else if (message.getMentionedUsers().get(0).getId().equals(author.getId())) {
			channel.sendMessage("❌ | Você não pode trocar cartas com você mesmo.").queue();
			return;
		} else if (args.length < 4) {
			channel.sendMessage("❌ | Você precisa mencionar uma quantia de créditos ou uma carta, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada, `E` = evogear, `F` = campo) para realizar a troca.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().getIfPresent(message.getMentionedUsers().get(0).getId()) != null) {
			channel.sendMessage("❌ | Este usuário possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
			return;
		}

		User other = message.getMentionedUsers().get(0);
		String text = StringUtils.normalizeSpace(String.join(" ", ArrayUtils.subarray(args, 1, args.length)));
		if (Helper.regex(text, "([\\w- ]+[NnCcEeFf]){2}")) { //Trade
			if (args.length < 5) {
				channel.sendMessage("❌ | Você precisa mencionar uma carta, o tipo, qual carta você deseja e o tipo dela (`N` = normal, `C` = cromada, `E` = evogear, `F` = campo) para realizar a troca.").queue();
				return;
			}

			int[] types = {
					switch (args[2].toUpperCase()) {
						case "N", "C" -> 1;
						case "E" -> 2;
						case "F" -> 3;
						default -> -1;
					},
					switch (args[4].toUpperCase()) {
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

			Pair<CardType, Object>[] products = new Pair[]{
					switch (types[0]) {
						case 1 -> Pair.of(CardType.KAWAIPON, (Object) CardDAO.getCard(args[1], false));
						case 2 -> Pair.of(CardType.EVOGEAR, (Object) CardDAO.getEquipment(args[1]));
						default -> Pair.of(CardType.FIELD, (Object) CardDAO.getField(args[1]));
					},
					switch (types[1]) {
						case 1 -> Pair.of(CardType.KAWAIPON, (Object) CardDAO.getCard(args[3], false));
						case 2 -> Pair.of(CardType.EVOGEAR, (Object) CardDAO.getEquipment(args[3]));
						default -> Pair.of(CardType.FIELD, (Object) CardDAO.getField(args[3]));
					}
			};

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			Kawaipon target = KawaiponDAO.getKawaipon(other.getId());
			boolean yourFoil = args[2].equalsIgnoreCase("C");
			boolean hisFoil = args[4].equalsIgnoreCase("C");

			if (products[0].getRight() == null) {
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
			} else if (products[1].getRight() == null) {
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
				products[0] = Pair.of(products[0].getLeft(), new KawaiponCard((Card) products[0].getRight(), yourFoil));
			if (types[1] == 1)
				products[1] = Pair.of(products[1].getLeft(), new KawaiponCard((Card) products[1].getRight(), hisFoil));

			if (AccountDAO.getAccount(kp.getUid()).getLoan() > 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-transfer-with-loan")).queue();
				return;
			} else if (AccountDAO.getAccount(target.getUid()).getLoan() > 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-with-loan")).queue();
				return;
			}

			switch (types[0]) {
				case 1 -> {
					KawaiponCard kc = (KawaiponCard) products[0].getRight();
					if (target.getCards().contains(kc)) {
						channel.sendMessage("❌ | Ele/ela já possui essa carta!").queue();
						return;
					} else if (!kp.getCards().contains(kc)) {
						channel.sendMessage("❌ | Parece que você não possui essa carta!").queue();
						return;
					}
				}
				case 2 -> {
					Equipment e = (Equipment) products[0].getRight();
					if (Collections.frequency(target.getEquipments(), e) == 3) {
						channel.sendMessage("❌ | Ele/ela já possui 3 cópias desse equipamento!").queue();
						return;
					} else if (target.getEquipments().size() == 18) {
						channel.sendMessage("❌ | Ele/ela já possui 18 equipamentos!").queue();
						return;
					} else if (target.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() == 1 && e.getTier() == 4) {
						channel.sendMessage("❌ | Ele/ela já possui 1 equipamento tier 4!").queue();
						return;
					} else if (!kp.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Parece que você não possui esse equipamento!").queue();
						return;
					}
				}
				default -> {
					Field f = (Field) products[0].getRight();
					if (Collections.frequency(target.getFields(), f) == 3) {
						channel.sendMessage("❌ | Ele/ela já possui 3 cópias dessa arena!").queue();
						return;
					} else if (target.getFields().size() == 3) {
						channel.sendMessage("❌ | Ele/ela já possui 3 arenas!").queue();
						return;
					} else if (!kp.getFields().contains(f)) {
						channel.sendMessage("❌ | Parece que você não possui essa arena!").queue();
						return;
					}
				}
			}

			switch (types[1]) {
				case 1 -> {
					KawaiponCard kc = (KawaiponCard) products[1].getRight();
					if (kp.getCards().contains(kc)) {
						channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
						return;
					} else if (!target.getCards().contains(kc)) {
						channel.sendMessage("❌ | Ele/ela não possui essa carta!").queue();
						return;
					}
				}
				case 2 -> {
					Equipment e = (Equipment) products[1].getRight();
					if (Collections.frequency(kp.getEquipments(), e) == 3) {
						channel.sendMessage("❌ | Parece que você já possui 3 cópias desse equipamento!").queue();
						return;
					} else if (kp.getEquipments().size() == 18) {
						channel.sendMessage("❌ | Parece que você já possui 18 equipamentos!").queue();
						return;
					} else if (kp.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() == 1 && e.getTier() == 4) {
						channel.sendMessage("❌ | Parece que você já possui 1 equipamento tier 4!").queue();
						return;
					} else if (!target.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Ele/ela não possui esse equipamento!").queue();
						return;
					}
				}
				default -> {
					Field f = (Field) products[1].getRight();
					if (Collections.frequency(kp.getFields(), f) == 3) {
						channel.sendMessage("❌ | Parece que você já possui 3 cópias dessa arena!").queue();
						return;
					} else if (kp.getFields().size() == 3) {
						channel.sendMessage("❌ | Parece que você já possui 3 arenas!").queue();
						return;
					} else if (!target.getFields().contains(f)) {
						channel.sendMessage("❌ | Ele/ela não possui essa arena!").queue();
						return;
					}
				}
			}

			String[] names = {
					switch (types[0]) {
						case 1 -> ((KawaiponCard) products[0].getRight()).getName();
						case 2 -> ((Equipment) products[0].getRight()).getCard().getName();
						default -> ((Field) products[0].getRight()).getCard().getName();
					},
					switch (types[1]) {
						case 1 -> ((KawaiponCard) products[1].getRight()).getName();
						case 2 -> ((Equipment) products[1].getRight()).getCard().getName();
						default -> ((Field) products[1].getRight()).getCard().getName();
					}
			};
			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			Main.getInfo().getConfirmationPending().put(other.getId(), true);
			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja trocar a carta `" + names[0] + "` pela sua carta `" + names[1] + "`, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
								if (!mb.getId().equals(other.getId())) return;
								else if (!ShiroInfo.getHashes().remove(hash)) return;
								Main.getInfo().getConfirmationPending().invalidate(author.getId());
								Main.getInfo().getConfirmationPending().invalidate(other.getId());
								Kawaipon finalKp = KawaiponDAO.getKawaipon(author.getId());
								Kawaipon finalTarget = KawaiponDAO.getKawaipon(other.getId());

								switch (types[0]) {
									case 1 -> {
										KawaiponCard kc = (KawaiponCard) products[0].getRight();
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
										Equipment eq = (Equipment) products[0].getRight();
										if (!finalKp.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalTarget.getEquipments(), eq) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (eq.getTier() == 4 && finalTarget.getEquipments().stream().anyMatch(e -> e.getTier() == 4)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
											return;
										}

										finalKp.removeEquipment(eq);
										finalTarget.addEquipment(eq);
									}
									default -> {
										Field f = (Field) products[0].getRight();
										if (!finalKp.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalTarget.getFields(), f) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
											return;
										}

										finalKp.removeField(f);
										finalTarget.addField(f);
									}
								}

								switch (types[1]) {
									case 1 -> {
										KawaiponCard kc = (KawaiponCard) products[1].getRight();
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
										Equipment eq = (Equipment) products[1].getRight();
										if (!finalTarget.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalKp.getEquipments(), eq) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (eq.getTier() == 4 && finalKp.getEquipments().stream().anyMatch(e -> e.getTier() == 4)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
											return;
										}

										finalTarget.removeEquipment(eq);
										finalKp.addEquipment(eq);
									}
									default -> {
										Field f = (Field) products[1].getRight();
										if (!finalTarget.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalKp.getFields(), f) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
											return;
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
								ShiroInfo.getHashes().remove(hash);
								Main.getInfo().getConfirmationPending().invalidate(author.getId());
								Main.getInfo().getConfirmationPending().invalidate(other.getId());
							})
					);
		} else if (Helper.regex(text, "(\\d+)[ ]+[\\w- ]+[NnCcEeFf]")) { //Purchase
			int type = switch (args[3].toUpperCase()) {
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
					if (Collections.frequency(kp.getEquipments(), e) == 3) {
						channel.sendMessage("❌ | Parece que você já possui 3 cópias desse equipamento!").queue();
						return;
					} else if (kp.getEquipments().size() == 18) {
						channel.sendMessage("❌ | Parece que você já possui 18 equipamentos!").queue();
						return;
					} else if (kp.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() == 1 && e.getTier() == 4) {
						channel.sendMessage("❌ | Parece que você já possui 1 equipamento tier 4!").queue();
						return;
					} else if (!target.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Ele/ela não possui esse equipamento!").queue();
						return;
					}
				}
				default -> {
					Field f = (Field) product.getRight();
					if (Collections.frequency(kp.getFields(), f) == 3) {
						channel.sendMessage("❌ | Parece que você já possui 3 cópias dessa arena!").queue();
						return;
					} else if (kp.getFields().size() == 3) {
						channel.sendMessage("❌ | Parece que você já possui 3 arenas!").queue();
						return;
					} else if (!target.getFields().contains(f)) {
						channel.sendMessage("❌ | Ele/ela não possui essa arena!").queue();
						return;
					}
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
			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			Main.getInfo().getConfirmationPending().put(other.getId(), true);
			Pair<CardType, Object> finalProduct = product;
			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja comprar " + (type == 1 ? "sua carta" : type == 2 ? "seu equipamento" : "sua arena") + " `" + name + "` por " + price + " créditos, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
								if (!mb.getId().equals(other.getId())) return;
								else if (!ShiroInfo.getHashes().remove(hash)) return;
								Main.getInfo().getConfirmationPending().invalidate(author.getId());
								Main.getInfo().getConfirmationPending().invalidate(other.getId());
								Kawaipon finalKp = KawaiponDAO.getKawaipon(author.getId());
								Kawaipon finalTarget = KawaiponDAO.getKawaipon(other.getId());
								acc.removeCredit(price, this.getClass());
								tacc.addCredit(price, this.getClass());

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
									}
									case 2 -> {
										Equipment eq = (Equipment) finalProduct.getRight();
										if (!finalTarget.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalKp.getEquipments(), eq) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (eq.getTier() == 4 && finalKp.getEquipments().stream().anyMatch(e -> e.getTier() == 4)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
											return;
										}

										finalTarget.removeEquipment(eq);
										finalKp.addEquipment(eq);
									}
									default -> {
										Field f = (Field) finalProduct.getRight();
										if (!finalTarget.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalKp.getFields(), f) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
											return;
										}

										finalTarget.removeField(f);
										finalKp.addField(f);
									}
								}

								KawaiponDAO.saveKawaipon(finalKp);
								KawaiponDAO.saveKawaipon(finalTarget);
								AccountDAO.saveAccount(acc);
								AccountDAO.saveAccount(tacc);

								s.delete().flatMap(n -> channel.sendMessage("✅ | Troca concluída com sucesso!")).queue(null, Helper::doNothing);
							}), true, 1, TimeUnit.MINUTES,
							u -> Helper.equalsAny(u.getId(), author.getId(), other.getId()),
							ms -> {
								ShiroInfo.getHashes().remove(hash);
								Main.getInfo().getConfirmationPending().invalidate(author.getId());
								Main.getInfo().getConfirmationPending().invalidate(other.getId());
							})
					);
		} else if (Helper.regex(text, "[\\w- ]+[NnCcEeFf][ ]+(\\d+)")) { //Selling
			int type = switch (args[2].toUpperCase()) {
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
					if (Collections.frequency(target.getEquipments(), e) == 3) {
						channel.sendMessage("❌ | Ele/ela já possui 3 cópias desse equipamento!").queue();
						return;
					} else if (target.getEquipments().size() == 18) {
						channel.sendMessage("❌ | Ele/ela já possui 18 equipamentos!").queue();
						return;
					} else if (target.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() == 1 && e.getTier() == 4) {
						channel.sendMessage("❌ | Ele/ela já possui 1 equipamento tier 4!").queue();
						return;
					} else if (!kp.getEquipments().contains(e)) {
						channel.sendMessage("❌ | Parece que você não possui esse equipamento!").queue();
						return;
					}
				}
				default -> {
					Field f = (Field) product.getRight();
					if (Collections.frequency(target.getFields(), f) == 3) {
						channel.sendMessage("❌ | Ele/ela já possui 3 cópias dessa arena!").queue();
						return;
					} else if (target.getFields().size() == 3) {
						channel.sendMessage("❌ | Ele/ela já possui 3 arenas!").queue();
						return;
					} else if (!kp.getFields().contains(f)) {
						channel.sendMessage("❌ | Parece que você não possui essa arena!").queue();
						return;
					}
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
			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			Main.getInfo().getConfirmationPending().put(other.getId(), true);
			Pair<CardType, Object> finalProduct = product;
			channel.sendMessage(other.getAsMention() + ", " + author.getAsMention() + " deseja vender " + (type == 1 ? "a carta" : type == 2 ? "o equipamento" : "a arena") + " `" + name + "` por " + price + " créditos, você aceita essa transação?")
					.queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
								if (!mb.getId().equals(other.getId())) return;
								else if (!ShiroInfo.getHashes().remove(hash)) return;
								Main.getInfo().getConfirmationPending().invalidate(author.getId());
								Main.getInfo().getConfirmationPending().invalidate(other.getId());
								Kawaipon finalKp = KawaiponDAO.getKawaipon(author.getId());
								Kawaipon finalTarget = KawaiponDAO.getKawaipon(other.getId());
								acc.addCredit(price, this.getClass());
								tacc.removeCredit(price, this.getClass());

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
									}
									case 2 -> {
										Equipment eq = (Equipment) finalProduct.getRight();
										if (!finalKp.getEquipments().contains(eq)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui esse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalTarget.getEquipments(), eq) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias desse equipamento).")).queue(null, Helper::doNothing);
											return;
										} else if (eq.getTier() == 4 && finalTarget.getEquipments().stream().anyMatch(e -> e.getTier() == 4)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 1 equipamento tier 4).")).queue(null, Helper::doNothing);
											return;
										}

										finalKp.removeEquipment(eq);
										finalTarget.addEquipment(eq);
									}
									default -> {
										Field f = (Field) finalProduct.getRight();
										if (!finalKp.getFields().contains(f)) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + author.getName() + " não possui essa arena).")).queue(null, Helper::doNothing);
											return;
										} else if (Collections.frequency(finalTarget.getFields(), f) == 3) {
											s.delete().flatMap(n -> channel.sendMessage("❌ | Troca anulada (" + other.getName() + " já possui 3 copias dessa arena).")).queue(null, Helper::doNothing);
											return;
										}

										finalKp.removeField(f);
										finalTarget.addField(f);
									}
								}

								KawaiponDAO.saveKawaipon(finalKp);
								KawaiponDAO.saveKawaipon(finalTarget);
								AccountDAO.saveAccount(acc);
								AccountDAO.saveAccount(tacc);

								s.delete().flatMap(n -> channel.sendMessage("✅ | Troca concluída com sucesso!")).queue(null, Helper::doNothing);
							}), true, 1, TimeUnit.MINUTES,
							u -> Helper.equalsAny(u.getId(), author.getId(), other.getId()),
							ms -> {
								ShiroInfo.getHashes().remove(hash);
								Main.getInfo().getConfirmationPending().invalidate(author.getId());
								Main.getInfo().getConfirmationPending().invalidate(other.getId());
							})
					);
		}
	}
}
