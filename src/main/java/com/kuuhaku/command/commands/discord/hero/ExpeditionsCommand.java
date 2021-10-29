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
import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ExpeditionDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Reward;
import com.kuuhaku.model.persistent.Expedition;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "expedicoes",
		aliases = {"expeditions"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class ExpeditionsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (!h.hasArrived()) {
			channel.sendMessage("❌ | Este herói está em uma expedição.").queue();
			return;
		}

		Calendar cal = Calendar.getInstance();
		List<Expedition> pool = Helper.getRandomN(ExpeditionDAO.getExpeditions(), 3, 1, Helper.stringToLong(author.getId() + h.getId() + cal.get(Calendar.WEEK_OF_YEAR) + cal.get(Calendar.YEAR)));
		pool.add(ExpeditionDAO.getExpedition("DOJO"));

		Map<String, ThrowingBiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		for (int i = 0; i < pool.size(); i++) {
			Expedition e = pool.get(i);

			EmbedBuilder loot = new ColorlessEmbedBuilder()
					.setTitle("Possíveis espólios");

			for (Map.Entry<String, Object> entry : e.getRewards().entrySet()) {
				Reward rew = Reward.valueOf(entry.getKey());
				int val = (int) (double) entry.getValue();

				loot.addField(rew.toString(),
						switch (rew) {
							case XP -> "Até +" + Helper.separate(val) + " XP";
							case HP -> "Até +" + Helper.separate(val) + " HP";
							case EP -> "Até +" + Helper.separate(val) + " EP";
							case CREDIT -> "Até +" + Helper.separate(val) + " CR";
							case GEM -> "Até +" + Helper.separate(val) + " gemas";
							case EQUIPMENT -> val + "% de chance";
						}, true);
			}

			int chance = e.getSuccessChance(h);
			EmbedBuilder penalties = new EmbedBuilder()
					.setColor(Color.red)
					.setTitle("Possíveis penalidades");

			if (chance < 15)
				penalties.addField("Morte", Helper.VOID, true);
			if (chance < 33)
				penalties.addField("Penalidade de XP", Helper.VOID, true);
			if (chance < 66)
				penalties.addField("Penalidade de HP", Helper.VOID, true);

			buttons.put(Helper.getFancyNumber(i + 1), (mb, ms) -> {
				if (h.getEnergy() < 1) {
					channel.sendMessage("❌ | Seu herói está cansado (sem energia suficiente).").queue();
					return;
				} else if (h.getHp() < h.getMaxHp() / 4) {
					channel.sendMessage("❌ | Seu herói está muito ferido para ir em uma expedição (HP muito baixo).").queue();
					return;
				}

				Main.getInfo().getConfirmationPending().put(h.getUid(), true);
				channel.sendMessage(h.getName() + " irá em uma expedição para " + e + " por " + Helper.toStringDuration(e.getTime()) + ", deseja confirmar?")
						.setEmbeds(loot.build(), penalties.build())
						.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mem, msg) -> {
									h.setExpedition(e);
									KawaiponDAO.saveHero(h);

									Main.getInfo().getConfirmationPending().remove(author.getId());
									s.delete()
											.flatMap(d -> ms.delete())
											.flatMap(d -> channel.sendMessage("✅ | Herói enviado com sucesso!"))
											.queue();
								}), true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(h.getUid()),
								m -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			});
		}


		channel.sendMessageEmbeds(getEmbed(h, pool)).queue(s ->
				Pages.buttonize(s, buttons, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}

	private MessageEmbed getEmbed(Hero h, List<Expedition> pool) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Mestres disponíveis")
				.setDescription("Seu tier no Shoukan ranqueado afetará quais mestres estão dispostos a treinar seu herói. Rotaciona a cada dia.");

		for (int i = 0; i < pool.size(); i++) {
			Expedition e = pool.get(i);
			int chance = e.getSuccessChance(h);
			String diff;
			if (chance < 5)
				diff = "Tem mais chance de cuspir pro alto e matar uma mosca";
			else if (chance < 15)
				diff = "**SUICIDIO!**";
			else if (chance < 25)
				diff = "Muito difícil";
			else if (chance < 33)
				diff = "Difícil";
			else if (chance < 60)
				diff = "Médio";
			else if (chance < 85)
				diff = "Fácil";
			else
				diff = "Muito fácil";


			eb.addField(Helper.getFancyNumber(i + 1) + " | " + e
					, "Dificuldade: %s (Sucesso: %s%%)\nDuração: %s".formatted(
							diff,
							chance,
							Helper.toStringDuration(TimeUnit.MILLISECONDS.convert(e.getTime(), TimeUnit.MINUTES))
					), false
			);
		}

		return eb.build();
	}
}
