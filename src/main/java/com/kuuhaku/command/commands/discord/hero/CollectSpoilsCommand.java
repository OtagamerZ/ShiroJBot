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
import com.kuuhaku.model.enums.Reward;
import com.kuuhaku.model.persistent.Expedition;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Map;

@Command(
		name = "fotoheroi",
		aliases = {"heroimage", "heroimg"},
		usage = "req_file",
		category = Category.MISC
)
public class CollectSpoilsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (h.getExpedition() == null || !h.hasArrived()) {
			channel.sendMessage("❌ | Seu herói não retornou de uma expedição ainda.").queue();
			return;
		}

		Expedition e = h.getExpedition();

		int chance = e.getSuccessChance(h);
		if (Helper.chance(chance)) {
			EmbedBuilder eb = new EmbedBuilder()
					.setColor(Color.green)
					.setTitle("Espólios da espedição para " + e);

			for (Map.Entry<String, Object> entry : e.getRewards().entrySet()) {
				Reward rew = Reward.valueOf(entry.getKey());
				int val = (int) entry.getValue();

				eb.addField(rew.toString(),
						switch (rew) {
							case XP, CREDIT, GEM -> Helper.separate(rew.reward(h, val));
							case EQUIPMENT -> String.valueOf(rew.reward(h, val));
						}, true);
			}

			channel.sendMessage("\uD83E\uDDED | Seja bem-vindo(a) de volta " + h.getName() + "!")
					.setEmbeds(eb.build())
					.queue();
		} else {
			EmbedBuilder eb = new EmbedBuilder()
					.setColor(Color.red)
					.setTitle("A expedição para " + e + " fracassou");

			if (chance < 33 && Helper.chance(50)) {
				int max = h.getXp();
				int penalty = Helper.rng(max / 10, max / 8);
				h.setXp(h.getXp() - penalty);
				eb.addField("Penalidade de XP", "-" + penalty, true);
			}
			if (chance < 66 && Helper.chance(50)) {
				int max = h.getMaxHp();
				int penalty = Helper.rng(max / 5, max / 3);
				h.setHp(h.getHp() - penalty);
				eb.addField("Penalidade de HP", "-" + penalty, true);
			}

			channel.sendMessage("\uD83E\uDDED | Seja bem-vindo(a) de volta " + h.getName() + "!")
					.setEmbeds(eb.build())
					.queue();
		}

		h = KawaiponDAO.getHero(author.getId());
		assert h != null;
		h.arrive();
		KawaiponDAO.saveHero(h);
	}
}
