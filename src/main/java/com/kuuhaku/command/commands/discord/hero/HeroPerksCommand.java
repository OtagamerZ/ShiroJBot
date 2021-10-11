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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
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

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "perksheroi",
		aliases = {"heroperks"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class HeroPerksCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		Hero h = CardDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui um herói.").queue();
			return;
		} else if (h.getAvailablePerks() == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Perks de " + h.getName());

			for (Perk perk : h.getPerks()) {
				eb.addField(perk.getName(), perk.getDescription(), false);
			}

			channel.sendMessageEmbeds(eb.build()).queue();
			return;
		}

		Set<Perk> pool;
		if (h.getPerks().isEmpty())
			pool = EnumSet.allOf(Perk.class);
		else
			pool = EnumSet.complementOf(EnumSet.copyOf(h.getPerks()));

		List<Perk> perks = Helper.getRandomN(List.copyOf(pool), 3, 1, author.getIdLong() + h.getLevel());
		channel.sendMessageEmbeds(getEmbed(perks)).queue(s ->
				Pages.buttonize(s, new LinkedHashMap<>() {{
							put("1️⃣", (mb, ms) -> {
								if (h.getAvailablePerks() <= 0) {
									channel.sendMessage("❌ | Você não tem mais espaço para perks.").queue();
									return;
								}

								choosePerk(h, s, perks.get(0));
							});
							if (perks.size() > 1)
								put("2️⃣", (mb, ms) -> {
									if (h.getAvailablePerks() <= 0) {
										channel.sendMessage("❌ | Você não tem mais espaço para perks.").queue();
										return;
									}

									choosePerk(h, s, perks.get(1));
								});
							if (perks.size() > 2)
								put("3️⃣", (mb, ms) -> {
									if (h.getAvailablePerks() <= 0) {
										channel.sendMessage("❌ | Você não tem mais espaço para perks.").queue();
										return;
									}

									choosePerk(h, s, perks.get(2));
								});
						}}, true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId())
				));
	}

	private void choosePerk(Hero h, Message msg, Perk perk) {
		msg.getChannel().sendMessage("Você selecionou a perk `" + perk + "`, deseja confirmar (a escolha é permanente)?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							h.getPerks().add(perk);
							CardDAO.saveHero(h);

							s.delete()
									.flatMap(d -> msg.getChannel().sendMessage("✅ | Perk selecionada com sucesso!"))
									.flatMap(d -> msg.delete())
									.queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(h.getUid())
				));
	}

	private MessageEmbed getEmbed(List<Perk> perks) {
		return new ColorlessEmbedBuilder()
				.setTitle("Perks disponíveis")
				.addField("1️⃣ | " + perks.get(0), perks.get(0).getDescription(), false)
				.addField("2️⃣ | " + perks.get(1), perks.get(1).getDescription(), false)
				.addField("3️⃣ | " + perks.get(2), perks.get(2).getDescription(), false)
				.build();
	}
}
