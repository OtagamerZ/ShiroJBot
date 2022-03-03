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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.BountyQuestDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Perk;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.Achievement;
import com.kuuhaku.model.enums.Danger;
import com.kuuhaku.model.enums.Event;
import com.kuuhaku.model.enums.Reward;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.BountyQuest;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.records.BountyInfo;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Map;

@Command(
		name = "recompensa",
		aliases = {"reward"},
		category = Category.MISC
)
public class BountyRewardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (h.getQuest() == null || !h.hasArrived()) {
			channel.sendMessage("❌ | Seu herói não retornou de uma missão ainda.").queue();
			return;
		}

		BountyInfo info = h.getQuest();
		BountyQuest q = BountyQuestDAO.getBounty(info.id());

		int lvl = h.getLevel();
		int diff = q.getDifficulty().getValue();
		double modDiff = Helper.prcnt(diff - info.diff(), diff);

		boolean died = false;
		EmbedBuilder eb = new EmbedBuilder();
		if (info.diff() == 0 || Helper.chance(100 * modDiff)) {
			eb.setColor(Color.green);

			boolean padoru = Event.getCurrent() == Event.XMAS;
			if (padoru)
				eb.setTitle("Recompensas da missão \"" + info + "\" (Bônus padoru)");
			else
				eb.setTitle("Recompensas da missão \"" + info + "\"");

			for (Map.Entry<Reward, Integer> e : info.rewards().entrySet()) {
				Reward rew = e.getKey();
				int val = Math.round(e.getValue() * (padoru ? 1.5f : 1));
				if (val == 0) continue;

				eb.addField(rew.toString(),
						switch (rew) {
							case XP -> Helper.separate(rew.apply(h, val)) + " XP";
							case EP -> Helper.separate(rew.apply(h, val)) + " EP";
							case CREDIT -> Helper.separate(rew.apply(h, val)) + " CR";
							case GEM -> Helper.separate(rew.apply(h, val)) + " gemas";
							case EQUIPMENT, SPELL -> String.valueOf(rew.apply(h, Helper.clamp(val, 0, 100)));
						}, true);
			}

			h = KawaiponDAO.getHero(author.getId());
			assert h != null;
		} else {
			eb.setColor(Color.red)
					.setTitle("A missão \"" + info + "\" fracassou");

			boolean opt = h.getPerks().contains(Perk.OPTIMISTIC);
			int expXp = (int) Math.round(info.rewards().getOrDefault(Reward.XP, 0) / 2d * (opt ? 1.25 : 1));

			if (expXp > 0 && Helper.chance(66)) {
				expXp = Helper.rng(expXp);

				if (expXp > 0) {
					h.setXp(h.getXp() + expXp);
					if (opt)
						eb.addField("Bônus de experiência", "+" + expXp + " XP (Otimista)", true);
					else
						eb.addField("Bônus de experiência", "+" + expXp + " XP", true);
				}
			}

			boolean pes = h.getPerks().contains(Perk.PESSIMISTIC);
			for (Danger danger : q.getDangers()) {
				if (Helper.chance(50 - (pes ? 10 : 0))) {
					switch (danger) {
						case EP -> {
							if (h.getEnergy() <= 1) continue;

							h.removeEnergy(1);
							eb.addField(danger.toString(), "-1 EP", true);
						}
						case XP -> {
							int max = h.getXp();
							int penalty = Helper.rng(max / 10, max / 8);
							h.setXp(h.getXp() - penalty);
							eb.addField(danger.toString(), "-" + penalty + " XP", true);
						}
						case DEATH -> {
							Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
							kp.getHeroes().remove(h);
							KawaiponDAO.saveKawaipon(kp);
							eb.addField(danger.toString(), "Seu herói morreu durante a missão", true);
							died = true;
						}
						case EQUIPMENT -> {
							if (h.getInventory().isEmpty()) continue;

							h.getInventory().remove(Helper.getRandomEntry(h.getInventory()));
							eb.addField(danger.toString(), "Seu herói perdeu um dos equipamentos durante a missão", true);
						}
					}
				}
			}
		}

		Account acc = AccountDAO.getAccount(author.getId());
		boolean save = false;

		if (h.getLevel() >= 10) {
			save = acc.getAchievements().add(Achievement.GROWING_STRONGER);
		} else if (h.getLevel() >= 20) {
			save = acc.getAchievements().add(Achievement.LEGENDARY_HERO);
		}

		if (save) AccountDAO.saveAccount(acc);

		if (!died && h.getLevel() > lvl) {
			h.setEnergy(h.getMaxEnergy());
			channel.sendMessage("\uD83E\uDDED | Seja bem-vindo(a) de volta " + h.getName() + "! **(+1 nível)**")
					.setEmbeds(eb.build())
					.queue();
		} else {
			channel.sendMessage("\uD83E\uDDED | Seja bem-vindo(a) de volta " + h.getName() + "!")
					.setEmbeds(eb.build())
					.queue();
		}

		h.arrive();
		if (!died) KawaiponDAO.saveHero(h);
	}
}
