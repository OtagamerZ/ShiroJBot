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

package com.kuuhaku.command.commands.discord.hero;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Kawaipon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

@Command(
		name = "selecionarheroi",
		aliases = {"selecthero", "sh"},
		usage = "req_id-opt",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class SelectHeroCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = Kawaipon.find(Kawaipon.class, author.getId());
		List<Hero> heroes = kp.getHeroes();

		if (heroes.isEmpty()) {
			channel.sendMessage("❌ | Você ainda não invocou nenhum herói.").queue();
			return;
		}

		if (args.length == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Heróis");

			for (int j = 0; j < heroes.size(); j++) {
				Hero h = heroes.get(j);

				String status = "";
				if (h.getQuest() != null) {
					if (h.hasArrived())
						status = "**(RETORNOU)** ";
					else
						status = "**(MISSÃO)** ";
				} else if (h.isResting())
					status = "**(DESCANSANDO)** ";

				eb.addField(
						"`Herói %s%s | %ssh %s`".formatted(
								h.getId(),
								heroes.indexOf(h) == kp.getActiveHero() ? " (ATUAL)" : "",
								prefix,
								h.getId()
						),
						status + h.getName() + "\n" + h.getRace() + "\nLevel " + h.getLevel(),
						true);
			}

			channel.sendMessageEmbeds(eb.build()).queue();
			return;
		}

		try {
			int id = Integer.parseInt(args[0]);
			Hero chosen = heroes.stream()
					.filter(h -> h.getId() == id)
					.findFirst().orElse(null);

			if (chosen == null) {
				channel.sendMessage("❌ | Herói inválido.").queue();
				return;
			} else if (id == kp.getActiveHero()) {
				channel.sendMessage("❌ | Este já é seu herói atual.").queue();
				return;
			}

			chosen.setResting(false);
			kp.setHero(id);
			kp.save();

			channel.sendMessage("✅ | Herói alternado com sucesso.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Herói inválido.").queue();
		}
	}
}
