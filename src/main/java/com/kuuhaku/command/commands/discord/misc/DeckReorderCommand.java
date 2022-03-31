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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Deck;
import net.dv8tion.jda.api.entities.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

@Command(
		name = "organizardeck",
		aliases = {"reorderdeck", "sortdeck", "reordenardeck", "odeck"},
		usage = "req_from-to-type-order",
		category = Category.MISC
)
public class DeckReorderCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Deck dk = KawaiponDAO.getDeck(author.getId());

		switch (args.length) {
			case 0 -> {
				channel.sendMessage("❌ | Você precisa informar 2 posições para trocar ou um tipo de ordem.").queue();
				return;
			}
			case 1 -> {
				String type = args[0];
				Comparator<Drawable> comp = switch (type.toLowerCase(Locale.ROOT)) {
					case "alfa", "alfabetico", "abc" -> Comparator.comparing(d -> d.getCard().getName());
					case "custo", "mana" -> Comparator.<Drawable>comparingInt(d -> {
						if (d instanceof Champion c) {
							return c.getMana();
						} else if (d instanceof Evogear e) {
							return e.getMana();
						} else {
							return 0;
						}
					}).reversed();
					case "atk", "ataque", "dano" -> Comparator.<Drawable>comparingInt(d -> {
						if (d instanceof Champion c) {
							return c.getAtk();
						} else if (d instanceof Evogear e) {
							return e.getAtk();
						} else {
							return 0;
						}
					}).reversed();
					case "def", "defesa" -> Comparator.<Drawable>comparingInt(d -> {
						if (d instanceof Champion c) {
							return c.getDef();
						} else if (d instanceof Evogear e) {
							return e.getDef();
						} else {
							return 0;
						}
					}).reversed();
					case "atributos", "stats" -> Comparator.<Drawable>comparingDouble(d -> {
						if (d instanceof Champion c) {
							return (double) (c.getAtk() + c.getDef()) / Math.max(1, c.getMana());
						} else if (d instanceof Evogear e) {
							return (double) (e.getAtk() + e.getDef()) / Math.max(1, e.getMana());
						} else {
							return 0;
						}
					}).reversed();
					case "tier" -> Comparator.<Drawable>comparingInt(d -> {
						if (d instanceof Evogear e) {
							return e.getTier();
						} else {
							return 0;
						}
					}).reversed();
					default -> null;
				};

				if (comp == null) {
					channel.sendMessage("❌ | Você precisa informar um tipo válido de ordenação (`alfabetico`, `mana`, `ataque`, `defesa`, `stats` ou `tier`).").queue();
					return;
				}

				dk.getChampions().sort(comp);
				dk.getEquipments().sort(comp);
				dk.getFields().sort(comp);
			}
			default -> {
				try {
					int from = Integer.parseInt(args[0]);
					int to = Integer.parseInt(args[1]);

					if (from < 0 || to < 0) {
						channel.sendMessage("❌ | Você precisa informar 2 posições válidas no deck.").queue();
						return;
					}

					switch (args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "c") {
						case "c" -> {
							if (to >= dk.getChampions().size()) {
								dk.getChampions().add(dk.getChampions().remove(from));
							} else {
								Collections.swap(dk.getChampions(), from, to);
							}
						}
						case "e" -> {
							if (to >= dk.getEquipments().size()) {
								dk.getEquipments().add(dk.getEquipments().remove(from));
							} else {
								Collections.swap(dk.getEquipments(), from, to);
							}
						}
						case "f" -> {
							if (to >= dk.getFields().size()) {
								dk.getFields().add(dk.getFields().remove(from));
							} else {
								Collections.swap(dk.getFields(), from, to);
							}
						}
						default -> {
							channel.sendMessage("❌ | O tipo deve ser `c` (campeão), `e` (evogear) ou `f` (campo).").queue();
							return;
						}
					}
				} catch (NumberFormatException | IndexOutOfBoundsException e) {
					channel.sendMessage("❌ | Você precisa informar 2 posições válidas no deck.").queue();
					return;
				}
			}
		}

		dk.save();
		channel.sendMessage("Deck reorganizado com sucesso!").queue();
	}
}
