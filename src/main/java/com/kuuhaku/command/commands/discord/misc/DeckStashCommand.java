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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.DeckStashDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.DeckStash;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(
		name = "reserva",
		aliases = {"stash", "estoque"},
		usage = "req_slot",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class DeckStashCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length == 0) {
			List<DeckStash> stashes = DeckStashDAO.getStash(author.getId());

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Decks reserva (capacidade: " + acc.getStashCapacity() + " slots)");

			for (int j = 0; j < stashes.size(); j++) {
				DeckStash ds = stashes.get(j);
				Map<Class, Integer> count = new HashMap<>() {{
					put(Class.DUELIST, 0);
					put(Class.SUPPORT, 0);
					put(Class.TANK, 0);
					put(Class.SPECIALIST, 0);
					put(Class.NUKE, 0);
					put(Class.TRAP, 0);
					put(Class.LEVELER, 0);
				}};
				for (Champion c : ds.getChampions())
					count.compute(c.getCategory(), (cl, ct) -> ct == null ? 1 : ct + 1);

				count.remove(null);

				String[] data = new String[14];
				for (int i = 0; i < Class.values().length; i++) {
					int ct = count.getOrDefault(Class.values()[i], 0);
					data[i * 2] = String.valueOf(ct);
					data[i * 2 + 1] = ct != 1 ? "s" : "";
				}

				double manaCost = ListUtils.union(ds.getChampions(), ds.getEquipments())
						.stream()
						.mapToInt(d -> d instanceof Champion ? ((Champion) d).getMana() : ((Equipment) d).getMana())
						.filter(i -> i != 0)
						.average()
						.orElse(0);

				Pair<Race, Race> combo = ds.getCombo();
				eb.addField(
						"`Slot %s | %sreserva %s`".formatted(j, prefix, j),
						"""
								:crossed_swords: | Cartas Senshi: %s
								:large_orange_diamond: | Efeito primário: %s (%s)
								:small_orange_diamond: | Efeito secundário: %s (%s)
								:shield: | Peso EvoGear: %s
								:thermometer: | Custo médio de mana: %s
																	
								%s
								""".formatted(
								ds.getChampions().size(),
								combo.getLeft(),
								combo.getLeft().getMajorDesc(),
								combo.getRight(),
								combo.getRight().getMinorDesc(),
								ds.getEvoWeight(),
								Helper.round(manaCost, 2),
								"""
										:abacus: | Classes
											**├─Duelista:** %s carta%s
											**├─Tanque:** %s carta%s
											**├─Suporte:** %s carta%s
											**├─Nuker:** %s carta%s
											**├─Armadilha:** %s carta%s
											**├─Nivelador:** %s carta%s
											**└─Especialista:** %s carta%s
											""".formatted((Object[]) data)),
						true);
			}

			channel.sendMessage(eb.build()).queue();
			return;
		}

		try {
			int slot = Integer.parseInt(args[0]);

			if (slot < 0 || slot >= acc.getStashCapacity()) {
				channel.sendMessage("❌ | Slot inválido.").queue();
				return;
			}

			DeckStash ds = DeckStashDAO.getStash(author.getId()).get(slot);

			List<Champion> champions = new ArrayList<>(kp.getChampions());
			List<Equipment> equipments = new ArrayList<>(kp.getEquipments());
			List<Field> fields = new ArrayList<>(kp.getFields());
			List<Integer> destinyDraw = kp.getDestinyDraw();

			kp.setChampions(new ArrayList<>(ds.getChampions()));
			kp.setEquipments(new ArrayList<>(ds.getEquipments()));
			kp.setFields(new ArrayList<>(ds.getFields()));
			if (ds.getDestinyDraw() != null) kp.setDestinyDraw(ds.getDestinyDraw().toArray(Integer[]::new));
			else kp.setDestinyDraw(new Integer[0]);

			ds.setChampions(champions);
			ds.setEquipments(equipments);
			ds.setFields(fields);
			if (destinyDraw != null) ds.setDestinyDraw(destinyDraw.toArray(Integer[]::new));
			else ds.setDestinyDraw(new Integer[0]);

			DeckStashDAO.saveStash(ds);
			KawaiponDAO.saveKawaipon(kp);

			channel.sendMessage("✅ | Deck alternado com sucesso.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O número do slot precisa ser um valor inteiro.").queue();
		}
	}
}
