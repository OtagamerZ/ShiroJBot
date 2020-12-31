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
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.DeckStashDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.DeckStash;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeckStashCommand extends Command {

	public DeckStashCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public DeckStashCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public DeckStashCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public DeckStashCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length == 0) {
			List<Page> pages = new ArrayList<>();

			List<List<DeckStash>> lobby = Helper.chunkify(DeckStashDAO.getStash(author.getId()), 10);

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Decks reserva (capacidade: " + acc.getStashCapacity() + " slots)");

			for (List<DeckStash> chunk : lobby) {
				for (int j = 0; j < chunk.size(); j++) {
					DeckStash ds = chunk.get(j);
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

					eb.addField(
							"`Slot %s` | %sreserva %s\n".formatted(j, prefix, j),
							"""
									:crossed_swords: | Cartas Senshi: %s
									:shield: | Cartas EvoGear: %s
																		
									%s
									""".formatted(
									ds.getChampions().size(),
									ds.getEquipments().size(),
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
							false);
				}

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			int slot = Integer.parseInt(args[0]);

			if (slot < 0 || slot >= acc.getStashCapacity()) {
				channel.sendMessage("❌ | Slot inválido.").queue();
				return;
			}

			DeckStash ds = DeckStashDAO.getStash(author.getId()).get(slot);

			List<Champion> champions = kp.getChampions();
			List<Equipment> equipments = kp.getEquipments();
			List<Field> fields = kp.getFields();
			List<Integer> destinyDraw = kp.getDestinyDraw();

			kp.setChampions(ds.getChampions());
			kp.setEquipments(ds.getEquipments());
			kp.setFields(ds.getFields());
			if (ds.getDestinyDraw() != null) kp.setDestinyDraw(ds.getDestinyDraw().toArray(Integer[]::new));

			ds.setChampions(champions);
			ds.setEquipments(equipments);
			ds.setFields(fields);
			if (destinyDraw != null) ds.setDestinyDraw(destinyDraw.toArray(Integer[]::new));

			DeckStashDAO.saveStash(ds);
			KawaiponDAO.saveKawaipon(kp);

			channel.sendMessage(":white_check_mark: | Deck alternado com sucesso.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O número do slot precisa ser um valor inteiro.").queue();
		}
	}
}
