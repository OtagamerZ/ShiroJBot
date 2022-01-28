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
import com.kuuhaku.controller.postgresql.DynamicParameterDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(
		name = "sintetizar",
		aliases = {"synthesize", "synth"},
		usage = "req_cards-type",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class SynthesizeCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar 3 cartas para sintetizar (nomes separados por `;`) e o tipo da síntese (`n` = síntese normal, `c` = síntese cromada e `r` = resintetizar).").queue();
			return;
		} else if (!Helper.equalsAny(args[1], "n", "c", "r")) {
			channel.sendMessage("❌ | Você precisa informar o tipo da síntese (`n` = síntese normal, `c` = síntese cromada e `r` = resintetizar).").queue();
			return;
		}

		String[] names = args[0].split(";");
		if (names.length > 3) {
			channel.sendMessage("❌ | Você não pode usar mais que 3 cartas na síntese.").queue();
			return;
		}

		CardType type = switch (args[1].toLowerCase(Locale.ROOT)) {
			case "c" -> CardType.FIELD;
			case "r" -> CardType.EVOGEAR;
			default -> CardType.KAWAIPON;
		};

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Deck dk = kp.getDeck();

		if (names.length < 3) {
			channel.sendMessage(switch (type) {
				case FIELD -> "❌ | Você precisa informar 3 cartas para sintetizar um campo.";
				case EVOGEAR -> "❌ | Você precisa informar 3 cartas para resintetizar um evogear.";
				default -> "❌ | Você precisa informar 3 cartas para sintetizar um evogear.";
			}).queue();
			return;
		}

		List<Card> tributes = new ArrayList<>();
		for (String name : names) {
			name = name.trim();
			Card c = CardDAO.getRawCard(name);

			if (c == null) {
				channel.sendMessage("❌ | A carta `" + name.toUpperCase(Locale.ROOT) + "` não existe, você não quis dizer `" + Helper.didYouMean(name, Stream.of(CardDAO.getAllCardNames(), CardDAO.getAllEquipmentNames(), CardDAO.getAllFieldNames()).flatMap(Collection::stream).toArray(String[]::new)) + "`?").queue();
				return;
			} else if (switch (type) {
				case FIELD -> !kp.getCards().contains(new KawaiponCard(c, true));
				case EVOGEAR -> dk.getEquipment(c) == null;
				default -> !kp.getCards().contains(new KawaiponCard(c, false));
			}) {
				channel.sendMessage("❌ | Você só pode usar na síntese cartas que você possua.").queue();
				return;
			} else if (Helper.equalsAny(c.getRarity(), KawaiponRarity.FIELD, KawaiponRarity.FUSION, KawaiponRarity.ULTIMATE)) {
				channel.sendMessage("❌ | Carta inválida para síntese.").queue();
				return;
			}

			tributes.add(c);
		}

		int score = switch (type) {
			case FIELD -> tributes.stream().mapToInt(c -> c.getRarity().getIndex()).sum() * 2;
			case EVOGEAR -> tributes.stream().mapToInt(c -> dk.getEquipment(c).getTier()).sum();
			default -> tributes.stream().mapToInt(c -> c.getRarity().getIndex()).sum();
		};

		DynamicParameter dp = DynamicParameterDAO.getParam("freeSynth_" + author.getId());
		int freeRolls = NumberUtils.toInt(dp.getValue());
		boolean blessed = !DynamicParameterDAO.getValue(author.getId() + "_blessing").isBlank();

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		switch (type) {
			case FIELD -> {
				List<Field> pool = CardDAO.getAllAvailableFields();
				if (blessed) {
					pool = pool.subList(0, Math.min(10, pool.size()));
				}

				Field f = Helper.getRandomEntry(pool);

				channel.sendMessage("Você está prester a sintetizar um campo usando essas cartas **CROMADAS** (" + (freeRolls > 0 ? "possui " + freeRolls + " sínteses gratúitas" : "elas serão destruídas no processo") + "). Deseja continuar?")
						.queue(s -> {
									Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new HashMap<>();
									buttons.put(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
										Main.getInfo().getConfirmationPending().remove(author.getId());

										if (dp.getValue().isBlank()) {
											for (Card t : tributes) {
												kp.removeCard(new KawaiponCard(t, true));
											}
										} else {
											DynamicParameterDAO.clearParam("freeSynth_" + author.getId());
										}

										if (dk.checkFieldError(f) > 0) {
											int change = (int) Math.round((350 + (score * 1400 / 15f)) * 2.5);

											Account acc = AccountDAO.getAccount(author.getId());
											acc.addCredit(change, this.getClass());
											AccountDAO.saveAccount(acc);

											channel.sendMessage("❌ | Você já possui 3 campos, as cartas usadas cartas foram convertidas em " + Helper.separate(change) + " CR.").queue();
										} else {
											dk.addField(f);
											channel.sendMessage("✅ | Síntese realizada com sucesso, você obteve o campo **" + f.getCard().getName() + "**!").queue();
										}

										if (blessed) {
											DynamicParameterDAO.clearParam("blessing_" + author.getId());
										}

										KawaiponDAO.saveKawaipon(kp);

										s.delete().queue(null, Helper::doNothing);
									});

									Pages.buttonize(s, buttons, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
											u -> u.getId().equals(author.getId()),
											ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
									);
								}
						);
			}
			case EVOGEAR -> {
				List<Equipment> pool = CardDAO.getAllAvailableEquipments();
				if (blessed) {
					pool = pool.subList(0, Math.min(10, pool.size()));
				}

				Bag<Integer> bag = tributes.stream()
						.map(c -> dk.getEquipment(c).getTier())
						.collect(Collectors.toCollection(HashBag::new));
				List<Equipment> chosenTier = Helper.getRandom(pool.stream()
						.collect(Collectors.groupingBy(Equipment::getTier))
						.entrySet()
						.stream()
						.map(e -> Pair.create(e.getValue(), bag.getCount(e.getKey()) / 3d))
						.toList()
				);

				Equipment e = Helper.getRandomEntry(chosenTier);

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setTitle("Possíveis resultados")
						.addField(KawaiponRarity.COMMON.getEmote() + " | Evogear tier 1 (\uD83D\uDFCA)", "Chance de " + (Helper.round(bag.getCount(1) * 100 / 3d, 1)) + "%", false)
						.addField(KawaiponRarity.RARE.getEmote() + " | Evogear tier 2 (\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(bag.getCount(2) * 100 / 3d, 1)) + "%", false)
						.addField(KawaiponRarity.ULTRA_RARE.getEmote() + " | Evogear tier 3 (\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(bag.getCount(3) * 100 / 3d, 1)) + "%", false)
						.addField(KawaiponRarity.LEGENDARY.getEmote() + " | Evogear tier 4 (\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(bag.getCount(4) * 100 / 3d, 1)) + "%", false);

				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage("Você está prester a resintetizar um evogear usando essas cartas (" + (freeRolls > 0 ? "possui " + freeRolls + " sínteses gratúitas" : "elas serão destruídas no processo") + "). Deseja continuar?")
						.setEmbeds(eb.build())
						.queue(s -> {
									Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new HashMap<>();
									buttons.put(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
										Main.getInfo().getConfirmationPending().remove(author.getId());
										String tier = StringUtils.repeat("\uD83D\uDFCA", e.getTier());

										if (dp.getValue().isBlank()) {
											for (Card t : tributes) {
												dk.removeEquipment(dk.getEquipment(t));
											}
										} else {
											DynamicParameterDAO.clearParam("freeSynth_" + author.getId());
										}

										if (dk.checkEquipmentError(e) != 0) {
											int change = (int) Math.round((350 + (score * 1400 / 15f)) * (e.getTier() == 4 ? 3.5 : 2.5));

											Account acc = AccountDAO.getAccount(author.getId());
											acc.addCredit(change, this.getClass());
											AccountDAO.saveAccount(acc);

											channel.sendMessage(
													switch (dk.checkEquipmentError(e)) {
														case 1 -> "❌ | Você já possui 3 cópias de **" + e.getCard().getName() + "**! (" + tier + "), as cartas usadas foram convertidas em " + Helper.separate(change) + " CR.";
														case 2 -> "❌ | Você já possui 1 evogear tier 4, **" + e.getCard().getName() + "**! (" + tier + "), as cartas usadas foram convertidas em " + Helper.separate(change) + " CR.";
														case 3 -> "❌ | Você não possui mais espaços para evogears, as cartas usadas cartas foram convertidas em " + Helper.separate(change) + " CR.";
														default -> throw new IllegalStateException("Unexpected value: " + dk.checkEquipmentError(e));
													}
											).queue();
										} else {
											dk.addEquipment(e);
											channel.sendMessage("✅ | Síntese realizada com sucesso, você obteve o evogear **" + e.getCard().getName() + "**! (" + tier + ")").queue();
										}

										if (blessed) {
											DynamicParameterDAO.clearParam("blessing_" + author.getId());
										}

										KawaiponDAO.saveKawaipon(kp);

										s.delete().queue(null, Helper::doNothing);
									});

									Pages.buttonize(s, buttons, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
											u -> u.getId().equals(author.getId()),
											ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
									);
								}
						);
			}
			default -> {
				int max = 15;
				double base = (max - score) / 0.75 / (max - 3);

				double t3 = Math.max(0, 0.65 - base);
				double t4 = Math.max(0, (t3 * 15) / 65 - 0.05);
				double t1 = Math.max(0, base - t4 * 10);
				double t2 = Math.max(0, 0.85 - Math.abs(0.105 - t1 / 3) * 5 - t3);
				double[] tiers = Helper.sumToOne(t1, t2, t3, t4);

				List<Equipment> pool = CardDAO.getAllAvailableEquipments();
				if (blessed) {
					pool = pool.subList(0, Math.min(10, pool.size()));
				}

				List<Equipment> chosenTier = Helper.getRandom(pool.stream()
						.collect(Collectors.groupingBy(Equipment::getTier))
						.entrySet()
						.stream()
						.map(e -> Pair.create(e.getValue(), switch (e.getKey()) {
									case 1, 2, 3, 4 -> tiers[e.getKey() - 1];
									default -> 0d;
								})
						).toList()
				);

				Equipment e = Helper.getRandomEntry(chosenTier);

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setTitle("Possíveis resultados")
						.addField(KawaiponRarity.COMMON.getEmote() + " | Evogear tier 1 (\uD83D\uDFCA)", "Chance de " + (Helper.round(tiers[0] * 100, 1)) + "%", false)
						.addField(KawaiponRarity.RARE.getEmote() + " | Evogear tier 2 (\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(tiers[1] * 100, 1)) + "%", false)
						.addField(KawaiponRarity.ULTRA_RARE.getEmote() + " | Evogear tier 3 (\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(tiers[2] * 100, 1)) + "%", false)
						.addField(KawaiponRarity.LEGENDARY.getEmote() + " | Evogear tier 4 (\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(tiers[3] * 100, 1)) + "%", false);

				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage("Você está prester a sintetizar um evogear usando essas cartas (" + (freeRolls > 0 ? "possui " + freeRolls + " sínteses gratúitas" : "elas serão destruídas no processo") + "). Deseja continuar?")
						.setEmbeds(eb.build())
						.queue(s -> {
									Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new HashMap<>();
									buttons.put(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
										Main.getInfo().getConfirmationPending().remove(author.getId());
										String tier = StringUtils.repeat("\uD83D\uDFCA", e.getTier());

										if (dp.getValue().isBlank()) {
											for (Card t : tributes) {
												kp.removeCard(new KawaiponCard(t, false));
											}
										} else {
											DynamicParameterDAO.clearParam("freeSynth_" + author.getId());
										}

										if (dk.checkEquipmentError(e) != 0) {
											int change = (int) Math.round((350 + (score * 1400 / 15f)) * (e.getTier() == 4 ? 3.5 : 2.5));

											Account acc = AccountDAO.getAccount(author.getId());
											acc.addCredit(change, this.getClass());
											AccountDAO.saveAccount(acc);

											channel.sendMessage(
													switch (dk.checkEquipmentError(e)) {
														case 1 -> "❌ | Você já possui 3 cópias de **" + e.getCard().getName() + "**! (" + tier + "), as cartas usadas foram convertidas em " + Helper.separate(change) + " CR.";
														case 2 -> "❌ | Você já possui 1 evogear tier 4, **" + e.getCard().getName() + "**! (" + tier + "), as cartas usadas foram convertidas em " + Helper.separate(change) + " CR.";
														case 3 -> "❌ | Você não possui mais espaços para evogears, as cartas usadas cartas foram convertidas em " + Helper.separate(change) + " CR.";
														default -> throw new IllegalStateException("Unexpected value: " + dk.checkEquipmentError(e));
													}
											).queue();
										} else {
											dk.addEquipment(e);
											channel.sendMessage("✅ | Síntese realizada com sucesso, você obteve o evogear **" + e.getCard().getName() + "**! (" + tier + ")").queue();
										}

										if (blessed) {
											DynamicParameterDAO.clearParam("blessing_" + author.getId());
										}

										KawaiponDAO.saveKawaipon(kp);

										s.delete().queue(null, Helper::doNothing);
									});

									Pages.buttonize(s, buttons, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
											u -> u.getId().equals(author.getId()),
											ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
									);
								}
						);
			}
		}
	}
}
