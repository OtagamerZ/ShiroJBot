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
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

@Command(
		name = "selecionarheroi",
		aliases = {"selecthero", "sh"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS
})
public class SelectHeroCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		List<Hero> heroes = kp.getHeroes();

		if (args.length == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Heróis");

			for (int j = 0; j < heroes.size(); j++) {
				Hero h = heroes.get(j);

				eb.addField(
						"`Herói %s%s | %ssh %s`".formatted(
								j,
								heroes.indexOf(h) == kp.getActiveHero() ? " (ATUAL)" : "",
								prefix,
								String.valueOf(j)
						),
						h.getName() + " - " + h.getRace() + " - Level " + h.getLevel(),
						true);
			}

			channel.sendMessageEmbeds(eb.build()).queue();
			return;
		}

		try {
			int slot = Integer.parseInt(args[0]);

			if (!Helper.between(slot, 0, heroes.size())) {
				channel.sendMessage("❌ | Herói inválido.").queue();
				return;
			} else if (slot == kp.getActiveHero()) {
				channel.sendMessage("❌ | Este já é seu herói atual.").queue();
				return;
			}

			kp.setHero(slot);
			KawaiponDAO.saveKawaipon(kp);

			channel.sendMessage("✅ | Herói alternado com sucesso.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | Herói inválido.").queue();
		}
	}
}
