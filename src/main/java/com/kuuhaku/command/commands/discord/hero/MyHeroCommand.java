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
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Perk;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "heroi",
		aliases = {"hero"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS
})
public class MyHeroCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = CardDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui um herói.").queue();
			return;
		} else if (h.getImage() == null) {
			channel.sendMessage("❌ | Seu herói não possui uma imagem.").queue();
			return;
		}

		List<String> perks = h.getPerks().stream().map(Perk::toString).collect(Collectors.toList());
		for (int i = 0; i < h.getAvailablePerks(); i++) {
			perks.add("`Perk disponível`");
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Herói " + h.getName())
				.addField(":chart_with_upwards_trend: | Nível: " + h.getLevel(), """
						XP: %s
						HP: %s/%s%s
						""".formatted(
						h.getXp() + (h.getXpToNext() == -1 ? "" : "/" + h.getXpToNext()),
						h.getHp(),
						h.getStats().calcMaxHp(h.getPerks()),
						h.getDmg() > 0 ? "\n`recuperação total em " + (int) Math.ceil(h.getDmg() * 10f / h.getStats().calcMaxHp(h.getPerks())) + " dias`" : ""
				), true)
				.addField(":bar_chart: | Stats:", """
								STR: %s
								RES: %s
								AGI: %s
								WIS: %s
								CON: %s
								"""
								.formatted((Object[]) h.getStats().getStats())
						, true)
				.addField(":books: | Perks:", String.join("\n", perks), true)
				.setImage("attachment://hero.png");

		channel.sendMessageEmbeds(eb.build())
				.addFile(Helper.getBytes(h.toChampion().drawCard(false), "png"), "hero.png")
				.queue();
	}
}
