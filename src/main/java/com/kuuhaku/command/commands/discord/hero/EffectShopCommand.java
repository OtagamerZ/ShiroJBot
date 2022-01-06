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
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Perk;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "lojaefeitos",
		aliases = {"effectshop", "lojae", "shope"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class EffectShopCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = KawaiponDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
			return;
		} else if (h.isUnavailable()) {
			channel.sendMessage("❌ | Este herói está em uma missão.").queue();
			return;
		}

		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());

		int max = mmr.getTier().getTier() + 1;
		boolean manaless = h.getPerks().contains(Perk.MANALESS);

		List<Champion> masters = CardDAO.getAllChampionsWithEffect(!manaless && mmr.getTier().getTier() >= 5, Math.min(manaless ? 4 : max, max));
		Calendar cal = Calendar.getInstance();
		long seed = Helper.stringToLong(author.getId() + h.getId() + cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR)) + h.getSeed();

		List<Champion> pool = Helper.getRandomN(masters, 5, 1, seed);
		Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
		for (int i = 0; i < pool.size(); i++) {
			Champion c = pool.get(i);
			buttons.put(Helper.parseEmoji(Helper.getFancyNumber(i + 1)), wrapper -> {
				Main.getInfo().getConfirmationPending().put(h.getUid(), true);
				channel.sendMessage(h.getName() + " será treinado por " + c.getName() + ", deseja confirmar?")
						.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), w -> {
									h.setReferenceChampion(c.getId());
									KawaiponDAO.saveHero(h);

									Main.getInfo().getConfirmationPending().remove(author.getId());
									s.delete()
											.flatMap(d -> wrapper.getMessage().delete())
											.flatMap(d -> channel.sendMessage("✅ | Treinado com sucesso!"))
											.queue();
								}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(h.getUid()),
								m -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			});
		}

		channel.sendMessageEmbeds(getEmbed(pool)).queue(s ->
				Pages.buttonize(s, buttons, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}

	private MessageEmbed getEmbed(List<Champion> pool) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Mestres disponíveis")
				.setDescription("Seu tier no Shoukan ranqueado afetará quais mestres estão dispostos a treinar seu herói. Rotaciona a cada dia.");

		for (int i = 0; i < pool.size(); i++) {
			Champion c = pool.get(i);
			int cost = (c.getMana() + (c.isFusion() ? 5 : 0)) / 2;
			eb.addField(Helper.getFancyNumber(i + 1) + " :droplet: " + cost + " | Mestre: " + c.getName(), c.getDescription(), false);
		}

		return eb.build();
	}
}
