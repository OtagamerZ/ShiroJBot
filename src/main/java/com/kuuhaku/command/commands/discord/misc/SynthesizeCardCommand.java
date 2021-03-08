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
import com.github.ygimenez.model.ThrowingBiConsumer;
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
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "sintetizar",
		aliases = {"synthesize", "synth"},
		usage = "req_cards-type",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class SynthesizeCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar 3 cartas para sintetizar um equipamento (nomes separados por `;`) e o tipo da síntese (`n` = síntese normal e `c` = síntese cromada).").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		} else if (!Helper.equalsAny(args[1], "n", "c")) {
			channel.sendMessage("❌ | Você precisa informar o tipo da síntese (`n` = síntese normal e `c` = síntese cromada).").queue();
			return;
		}

		String[] names = args[0].split(";");
		boolean foilSynth = args[1].equalsIgnoreCase("c");
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (names.length < 3) {
			channel.sendMessage("❌ | Você precisa informar 3 cartas para sintetizar um" + (foilSynth ? "a arena" : " equipamento") + ".").queue();
			return;
		}

		List<Card> tributes = new ArrayList<>();
		for (String name : names) {
			Card c = CardDAO.getCard(name, false);
			if (c == null) {
				channel.sendMessage("❌ | A carta `" + name.toUpperCase() + "` não existe.").queue();
				return;
			} else if (foilSynth ? !kp.getCards().contains(new KawaiponCard(c, true)) : !kp.getCards().contains(new KawaiponCard(c, false))) {
				channel.sendMessage("❌ | Você só pode usar na síntese cartas que você tenha na coleção kawaipon.").queue();
				return;
			}

			tributes.add(c);
		}

		if (foilSynth) {
			int score = tributes.stream().mapToInt(c -> c.getRarity().getIndex()).sum() * 2;
			List<Field> fs = CardDAO.getAllAvailableFields();
			Field f = fs.get(Helper.rng(fs.size(), true));

			DynamicParameter dp = DynamicParameterDAO.getParam("freeSynth_" + author.getId());
			int freeRolls = NumberUtils.toInt(dp.getValue());

			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Você está prester a sintetizar uma arena usando essas cartas **CROMADAS** (" + (freeRolls > 0 ? "possui " + freeRolls + " sínteses gratúitas" : "elas serão destruídas no processo") + "). Deseja continuar?")
					.queue(s ->
							{
								Map<String, ThrowingBiConsumer<Member, Message>> buttons = new java.util.HashMap<>();
								buttons.put(Helper.ACCEPT, (ms, mb) -> {
									if (!ShiroInfo.getHashes().remove(hash)) return;
									Main.getInfo().getConfirmationPending().invalidate(author.getId());

									if (kp.getFields().size() == 3) {
										int change = (int) Math.round((350 + (score * 1400 / 15f)) * 2.5);

										Account acc = AccountDAO.getAccount(author.getId());
										acc.addCredit(change, this.getClass());
										AccountDAO.saveAccount(acc);

										if (kp.getFields().size() == 3)
											channel.sendMessage("❌ | Você já possui 3 campos, as cartas usadas cartas foram convertidas em " + Helper.separate(change) + " créditos.").queue();

										if (dp.getValue().isBlank()) {
											for (Card t : tributes) {
												kp.removeCard(new KawaiponCard(t, true));
											}
										} else if (freeRolls > 0)
											DynamicParameterDAO.setParam("freeSynth_" + author.getId(), String.valueOf(freeRolls - 1));
										else
											DynamicParameterDAO.clearParam("freeSynth_" + author.getId());

										KawaiponDAO.saveKawaipon(kp);
										s.delete().queue(null, Helper::doNothing);
										return;
									}

									kp.addField(f);

									if (dp.getValue().isBlank()) {
										for (Card t : tributes) {
											kp.removeCard(new KawaiponCard(t, true));
										}
									} else
										DynamicParameterDAO.clearParam("freeSynth_" + author.getId());

									KawaiponDAO.saveKawaipon(kp);

									s.delete().queue(null, Helper::doNothing);
									channel.sendMessage("✅ | Síntese realizada com sucesso, você obteve a arena **" + f.getCard().getName() + "**!").queue();
								});
								Pages.buttonize(s, buttons, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()), ms -> {
									ShiroInfo.getHashes().remove(hash);
									Main.getInfo().getConfirmationPending().invalidate(author.getId());
								});
							}
					);
		} else {
			int score = tributes.stream().mapToInt(c -> c.getRarity().getIndex()).sum();
			double tier1 = (15 - score) * 0.75 / 12;
			double tier2 = 0.25 + (6 - Math.abs(9 - score)) * 0.25 / 6;
			double tier3 = Math.max(0, 0.65 - tier1);
			double tier4 = tier3 * 0.1 / 0.65;

			List<Equipment> equips = CardDAO.getAllEquipments();
			List<Equipment> chosenTier = Helper.getRandom(List.of(
					Pair.create(equips.stream().filter(eq -> eq.getTier() == 1).collect(Collectors.toList()), tier1),
					Pair.create(equips.stream().filter(eq -> eq.getTier() == 2).collect(Collectors.toList()), tier2),
					Pair.create(equips.stream().filter(eq -> eq.getTier() == 3).collect(Collectors.toList()), tier3),
					Pair.create(equips.stream().filter(eq -> eq.getTier() == 4).collect(Collectors.toList()), tier4)
			));

			Equipment e = chosenTier.get(Helper.rng(chosenTier.size(), true));

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Possíveis resultados")
					.addField(KawaiponRarity.COMMON.getEmote() + " | Equipamento tier 1 (\uD83D\uDFCA)", "Chance de " + (Helper.round(tier1 * 100, 1)) + "%", false)
					.addField(KawaiponRarity.RARE.getEmote() + " | Equipamento tier 2 (\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(tier2 * 100, 1)) + "%", false)
					.addField(KawaiponRarity.ULTRA_RARE.getEmote() + " | Equipamento tier 3 (\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(tier3 * 100, 1)) + "%", false)
					.addField(KawaiponRarity.LEGENDARY.getEmote() + " | Equipamento tier 4 (\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA\uD83D\uDFCA)", "Chance de " + (Helper.round(tier4 * 100, 1)) + "%", false);

			DynamicParameter dp = DynamicParameterDAO.getParam("freeSynth_" + author.getId());
			int freeRolls = NumberUtils.toInt(dp.getValue());

			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Você está prester a sintetizar um equipamento usando essas cartas (" + (freeRolls > 0 ? "possui " + freeRolls + " sínteses gratúitas" : "elas serão destruídas no processo") + "). Deseja continuar?")
					.embed(eb.build())
					.queue(s ->
							{
								Map<String, ThrowingBiConsumer<Member, Message>> buttons = new java.util.HashMap<>();
								buttons.put(Helper.ACCEPT, (ms, mb) -> {
									if (!ShiroInfo.getHashes().remove(hash)) return;
									Main.getInfo().getConfirmationPending().invalidate(author.getId());
									String tier = StringUtils.repeat("\uD83D\uDFCA", e.getTier());

									if (kp.getEquipments().stream().filter(e::equals).count() == 3 || (kp.getEquipments().stream().filter(eq -> eq.getTier() == 4).count() >= 1 && e.getTier() == 4) || kp.getEvoWeight() + e.getWeight(kp) > 24) {
										int change = (int) Math.round((350 + (score * 1400 / 15f)) * (e.getTier() == 4 ? 3.5 : 2.5));

										Account acc = AccountDAO.getAccount(author.getId());
										acc.addCredit(change, this.getClass());
										AccountDAO.saveAccount(acc);

										channel.sendMessage(
												switch (kp.checkEquipmentError(e)) {
													case 1 -> "❌ | Você já possui 3 cópias de **" + e.getCard().getName() + "**! (" + tier + "), as cartas usadas foram convertidas em " + Helper.separate(change) + " créditos.";
													case 2 -> "❌ | Você já possui 1 equipamento tier 4, **" + e.getCard().getName() + "**! (" + tier + "), as cartas usadas foram convertidas em " + Helper.separate(change) + " créditos.";
													case 3 -> "❌ | Você não possui mais espaços para equipamentos, as cartas usadas cartas foram convertidas em " + Helper.separate(change) + " créditos.";
													default -> throw new IllegalStateException("Unexpected value: " + kp.checkEquipmentError(e));
												}
										).queue();

										if (dp.getValue().isBlank()) {
											for (Card t : tributes) {
												kp.removeCard(new KawaiponCard(t, false));
											}
										} else if (freeRolls > 0)
											DynamicParameterDAO.setParam("freeSynth_" + author.getId(), String.valueOf(freeRolls - 1));
										else
											DynamicParameterDAO.clearParam("freeSynth_" + author.getId());

										KawaiponDAO.saveKawaipon(kp);
										s.delete().queue(null, Helper::doNothing);
										return;
									}

									kp.addEquipment(e);

									if (dp.getValue().isBlank()) {
										for (Card t : tributes) {
											kp.removeCard(new KawaiponCard(t, false));
										}
									} else
										DynamicParameterDAO.clearParam("freeSynth_" + author.getId());

									KawaiponDAO.saveKawaipon(kp);

									s.delete().queue(null, Helper::doNothing);
									channel.sendMessage("✅ | Síntese realizada com sucesso, você obteve o equipamento **" + e.getCard().getName() + "**! (" + tier + ")").queue();
								});
								Pages.buttonize(s, buttons, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()), ms -> {
									ShiroInfo.getHashes().remove(hash);
									Main.getInfo().getConfirmationPending().invalidate(author.getId());
								});
							}
					);
		}
	}
}
