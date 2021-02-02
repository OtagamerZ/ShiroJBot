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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(
		name = "avaliardeck",
		aliases = {"deckeval"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class DeckEvalCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		Map<Class, Integer> count = new HashMap<>() {{
			put(Class.DUELIST, 0);
			put(Class.SUPPORT, 0);
			put(Class.TANK, 0);
			put(Class.SPECIALIST, 0);
			put(Class.NUKE, 0);
			put(Class.TRAP, 0);
			put(Class.LEVELER, 0);
		}};
		for (Champion c : kp.getChampions())
			count.compute(c.getCategory(), (cl, ct) -> ct == null ? 1 : ct + 1);

		count.remove(null);

		String[] data = new String[14];
		for (int i = 0; i < Class.values().length; i++) {
			int ct = count.getOrDefault(Class.values()[i], 0);
			data[i * 2] = String.valueOf(ct);
			data[i * 2 + 1] = ct != 1 ? "s" : "";
		}

		double manaCost = ListUtils.union(kp.getChampions(), kp.getEquipments())
				.stream()
				.mapToInt(d -> d instanceof Champion ? ((Champion) d).getMana() : ((Equipment) d).getMana())
				.filter(i -> i != 0)
				.average()
				.orElse(0);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Análise do deck de " + author.getName())
				.addField(
						":crossed_swords: | Cartas Senshi: " + kp.getChampions().size(),
						":shield: | Cartas EvoGear: %s\n:thermometer: | Custo médio de mana: %s".formatted(kp.getEquipments().size(), Helper.round(manaCost, 2)),
						false
				)
				.addField(":abacus: | Classes", """
						**Duelista:** %s carta%s
						**Tanque:** %s carta%s
						**Suporte:** %s carta%s
						**Nuker:** %s carta%s
						**Armadilha:** %s carta%s
						**Nivelador:** %s carta%s
						**Especialista:** %s carta%s
						""".formatted((Object[]) data), false);

		List<String> tips = new ArrayList<>();
		for (Map.Entry<Class, Integer> e : count.entrySet()) {
			switch (e.getKey()) {
				case DUELIST -> {
					if (e.getValue() < 6)
						tips.add("É importante ter várias cartas do tipo duelista, pois elas costumam ser as mais baratas e oferecem versatilidade durante as partidas.");
				}
				case SUPPORT -> {
					if (e.getValue() < 3)
						tips.add("Decks que possuem cartas de suporte costumam sobressair em partidas extensas, lembre-se que nem sempre dano é o fator vitorioso.");
				}
				case TANK -> {
					if (e.getValue() < 3)
						tips.add("Um deck sem tanques possui uma defesa muito fraca, lembre-se que após cada turno será a vez do oponente.");
				}
				case SPECIALIST -> {
					if (e.getValue() < 1)
						tips.add("Apesar de serem cartas situacionais, as cartas-especialista são essenciais em qualquer deck pois nunca se sabe que rumo a partida irá tomar.");
				}
				case NUKE -> {
					if (e.getValue() < 1)
						tips.add("Existem cartas com alto ataque ou defesa, seu deck estará vulnerável sem uma carta para explodi-las.");
				}
				case TRAP -> {
					if (e.getValue() < 5)
						tips.add("Sem cartas-armadilha à sua disposição, o oponente não precisará se preocupar em atacar cartas viradas para baixo, o que te torna um alvo fácil.");
				}
				case LEVELER -> {
					if (e.getValue() < 5)
						tips.add("Cartas niveladoras são essenciais para defender-se de um turno ruim, não subestime o poder delas.");
				}
			}
		}

		if (manaCost >= 3.5)
			tips.add("Seu deck possui um custo de mana muito alto. Apesar das cartas de custo alto serem mais forte, não adianta nada se você conseguir invocar apenas 1 por turno.");

		if (kp.getChampions().size() < 30)
			eb.setDescription("Seu deck ainda não está pronto para duelos.");
		else
			eb.setDescription(tips.size() == 0 ? "Seu deck está bem distribuído, parabéns!" : String.join("\n\n", tips));

		channel.sendMessage(eb.build()).queue();
	}
}
