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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

@Command(
		name = "decks",
		aliases = {"alternar", "stash", "switch"},
		usage = "req_slot",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class DeckStashCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = Kawaipon.find(Kawaipon.class, author.getId());
		Account acc = Account.find(Account.class, author.getId());
		List<Deck> decks = kp.getDecks();

		if (args.length == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Decks reserva (capacidade: " + acc.getDeckStashCapacity() + " slots)");

			for (int j = 0; j < decks.size(); j++) {
				Deck dk = decks.get(j);

				eb.addField(
						"`Slot %s%s | %salternar %s`".formatted(
								j,
								decks.indexOf(dk) == kp.getActiveDeck() ? " (ATUAL)" : "",
								prefix,
								CollectionHelper.getOr(dk.getName(), String.valueOf(j))
						),
						dk.toString(),
						true);
			}

			channel.sendMessageEmbeds(eb.build()).queue();
			return;
		}

		try {
			int slot = Integer.parseInt(args[0]);

			if (!MathHelper.between(slot, 0, decks.size())) {
				channel.sendMessage("❌ | Slot inválido.").queue();
				return;
			} else if (slot == kp.getActiveDeck()) {
				channel.sendMessage("❌ | Este já é seu deck atual.").queue();
				return;
			}

			kp.setDeck(slot);
			kp.save();

			channel.sendMessage("✅ | Deck alternado com sucesso.").queue();
		} catch (NumberFormatException e) {
			Deck dk = decks.stream()
					.filter(d -> d.getName() != null)
					.filter(d -> d.getName().equalsIgnoreCase(args[0]))
					.findFirst()
					.orElse(null);
			if (dk == null) {
				channel.sendMessage("❌ | Nenhum deck com o nome `" + StringHelper.bugText(args[0]) + "` encontrado.").queue();
				return;
			}

			int slot = decks.indexOf(dk);
			if (slot == kp.getActiveDeck()) {
				channel.sendMessage("❌ | Este já é seu deck atual.").queue();
				return;
			}

			kp.setDeck(slot);
			kp.save();

			channel.sendMessage("✅ | Deck alternado com sucesso.").queue();
		}
	}
}
