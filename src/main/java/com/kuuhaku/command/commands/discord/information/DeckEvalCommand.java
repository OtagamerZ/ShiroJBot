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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Kawaipon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Map;

public class DeckEvalCommand extends Command {

	public DeckEvalCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public DeckEvalCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public DeckEvalCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public DeckEvalCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
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

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Análise do deck de " + author.getName())
				.addField(":crossed_swords: | Cartas Senshi: " + kp.getChampions().size(), ":shield: | Cartas EvoGear: " + kp.getEquipments().size(), false)
				.addField(":abacus: | Classes", """
						**Duelista:** %s carta%s
						**Tanque:** %s carta%s
						**Suporte:** %s carta%s
						**Nuker:** %s carta%s
						**Armadilha:** %s carta%s
						**Nivelador:** %s carta%s
						**Especialista:** %s carta%s
						""".formatted((Object[]) data), false);

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Class, Integer> e : count.entrySet()) {
			switch (e.getKey()) {
				case DUELIST -> {
					if (e.getValue() < 6)
						sb.append("É importante ter várias cartas do tipo duelista, pois elas costumam ser as mais baratas e oferecem versatilidade durante as partidas.\n\n");
				}
				case SUPPORT -> {
					if (e.getValue() < 3)
						sb.append("Decks que possuem cartas de suporte costumam sobressair em partidas extensas, lembre-se que nem sempre dano é o fator vitorioso.\n\n");
				}
				case TANK -> {
					if (e.getValue() < 3)
						sb.append("Um deck sem tanques possui uma defesa muito fraca, lembre-se que após cada turno será a vez do oponente.\n\n");
				}
				case SPECIALIST -> {
					if (e.getValue() < 1)
						sb.append("Apesar de serem cartas situacionais, as cartas-especialista são essenciais em qualquer deck pois nunca se sabe que rumo a partida irá tomar.\n\n");
				}
				case NUKE -> {
					if (e.getValue() < 1)
						sb.append("Existem cartas com alto ataque ou defesa, seu deck estará vulnerável sem uma carta para explodi-las.\n\n");
				}
				case TRAP -> {
					if (e.getValue() < 5)
						sb.append("Sem cartas-armadilha à sua disposição, o oponente não precisará se preocupar em atacar cartas viradas para baixo, o que te torna um alvo fácil.\n\n");
				}
				case LEVELER -> {
					if (e.getValue() < 5)
						sb.append("Cartas niveladoras são essenciais para defender-se de um turno ruim, não subestime o poder delas.\n\n");
				}
			}
		}

		if (kp.getChampions().size() < 30)
			eb.setDescription("Seu deck ainda não está pronto para duelos.");
		else
			eb.setDescription(sb.toString().isBlank() ? "Seu deck está bem distribuído, parabéns!" : StringUtils.trim(sb.toString()));

		channel.sendMessage(eb.build()).queue();
	}
}
