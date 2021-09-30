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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Perk;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
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
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class EffectShopCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Hero h = CardDAO.getHero(author.getId());

		if (h == null) {
			channel.sendMessage("❌ | Você não possui um herói.").queue();
			return;
		}

		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());

		Calendar cal = Calendar.getInstance();
		List<Champion> pool = Helper.getRandomN(CardDAO.getAllChampionsWithEffect(mmr.getTier().getTier() >= 5, mmr.getTier().getTier() + 1), 5, 1, author.getIdLong() + cal.get(Calendar.WEEK_OF_YEAR) + cal.get(Calendar.YEAR));
		Map<String, ThrowingBiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		for (int i = 0; i < pool.size(); i++) {
			Champion c = pool.get(i);
			buttons.put(Helper.getFancyNumber(i + 1), (mb, ms) -> {
				Account acc = AccountDAO.getAccount(author.getId());
				if (acc.getGems() < c.getMana()) {
					channel.sendMessage("❌ | Você não possui gemas suficientes para pagar o treinamento.").queue();
					return;
				}

				Main.getInfo().getConfirmationPending().put(h.getUid(), true);
				channel.sendMessage(h.getName() + " será treinado por " + c.getName() + " por " + c.getMana() + " gemas, deseja confirmar?")
						.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mem, msg) -> {
									h.setReferenceChampion(c.getId());
									CardDAO.saveHero(h);

									Main.getInfo().getConfirmationPending().remove(author.getId());
									s.delete().flatMap(d -> channel.sendMessage("✅ | Treinado com sucesso!")).queue();
								}), true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(h.getUid()),
								m -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			});
		}


		channel.sendMessageEmbeds(getEmbed(pool)).queue(s ->
				Pages.buttonize(s, buttons, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
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

	private MessageEmbed getEmbed(List<Champion> pool) {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Mestres disponíveis")
				.setDescription("Seu tier no Shoukan ranqueado afetará quais mestres estão dispostos a treinar seu herói. Rotaciona a cada semana.");

		for (int i = 0; i < pool.size(); i++) {
			Champion c = pool.get(i);
			eb.addField(Helper.getFancyNumber(i + 1) + " :diamonds: " + c.getMana() + " | Mestre: " + c.getName(), c.getDescription(), false);
		}

		return eb.build();
	}
}
