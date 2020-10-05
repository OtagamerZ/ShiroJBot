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
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SynthesizeCardCommand extends Command {

	public SynthesizeCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SynthesizeCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SynthesizeCardCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SynthesizeCardCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa informar 3 cartas para sintetizar um equipamento.").queue();
			return;
		}

		String[] names = args[0].split(";");
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (names.length < 3) {
			channel.sendMessage("❌ | Você precisa informar 3 cartas para sintetizar um equipamento.").queue();
			return;
		}

		List<Card> tributes = new ArrayList<>();
		for (String name : names) {
			Card c = CardDAO.getCard(name, false);
			if (c == null) {
				channel.sendMessage("❌ | A carta `" + name.toUpperCase() + "` não existe.").queue();
				return;
			} else if (!kp.getCards().contains(new KawaiponCard(c, false))) {
				channel.sendMessage("❌ | Você só pode usar na síntese cartas que você tenha na coleção kawaipon.").queue();
				return;
			}
			tributes.add(c);
		}

		int score = tributes.stream().mapToInt(c -> c.getRarity().getIndex()).sum();
		double tier1 = (15 - score) * 0.75 / 12;
		double tier2 = (6 - Math.abs(9 - score)) * 0.5 / 6;
		double tier3 = 12 * 0.75 / (15 - score);

		List<Equipment> equips = CardDAO.getEquipments();
		List<Equipment> chosenTier = new EnumeratedDistribution<>(List.of(
				Pair.create(equips.stream().filter(eq -> eq.getTier() == 1).collect(Collectors.toList()), tier1),
				Pair.create(equips.stream().filter(eq -> eq.getTier() == 2).collect(Collectors.toList()), tier2),
				Pair.create(equips.stream().filter(eq -> eq.getTier() == 3).collect(Collectors.toList()), tier3)
		)).sample();

		Equipment e = chosenTier.get(Helper.rng(chosenTier.size(), true));

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Possíveis resultados")
				.addField("Equipamento tier 1", "Chance de " + (Helper.round(tier1 * 100, 1)) + "%", false)
				.addField("Equipamento tier 2", "Chance de " + (Helper.round(tier2 * 100, 1)) + "%", false)
				.addField("Equipamento tier 3", "Chance de " + (Helper.round(tier3 * 100, 1)) + "%", false);

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Você está prester a sintetizar um equipamento usando essas cartas (elas serão destruídas no processo). Deseja continual?")
				.embed(eb.build())
				.queue(s ->
						Pages.buttonize(s, Map.of(Helper.ACCEPT, (ms, mb) -> {
							if (!ShiroInfo.getHashes().remove(hash)) return;
							Main.getInfo().getConfirmationPending().invalidate(author.getId());

							if (kp.getEquipments().stream().filter(e::equals).count() == 3 || kp.getEquipments().size() == 18) {
								int change = (int) Math.round((350 + (score * 1400 / 15f)) * 2.5);

								Account acc = AccountDAO.getAccount(author.getId());
								acc.addCredit(change, this.getClass());
								AccountDAO.saveAccount(acc);

								if (kp.getEquipments().size() == 18)
									channel.sendMessage("❌ | Você já possui 18 equipamentos, suas cartas foram convertidas em " + change + " créditos.").queue();
								else
									channel.sendMessage("❌ | Você já possui 3 cópias desse equipamento, suas cartas foram convertidas em " + change + " créditos.").queue();
								return;
							}

							kp.addEquipment(e);
							KawaiponDAO.saveKawaipon(kp);

							channel.sendMessage("Síntese realizada com sucesso, você obteve o equipamento **" + e.getCard().getName() + "**!").queue();
						}), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()), ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}
}