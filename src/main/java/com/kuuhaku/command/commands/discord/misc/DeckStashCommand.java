/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Command(
		name = "reserva",
		aliases = {"stash", "estoque"},
		usage = "req_slot",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class DeckStashCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Account acc = AccountDAO.getAccount(author.getId());

		if (args.length == 0) {
			List<Deck> stashes = kp.getDecks();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Decks reserva (capacidade: " + acc.getStashCapacity() + " slots)");

			for (int j = 0; j < stashes.size(); j++) {
				Deck dk = stashes.get(j);

				Pair<Race, Race> combo = dk.getCombo();
				eb.addField(
						"`Slot %s%s | %sreserva %s`".formatted(
								j,
								kp.getDecks().indexOf(dk) == kp.getActiveDeck() ? " (ATUAL)" : "",
								prefix,
								Helper.getOr(dk.getName(), String.valueOf(j))),
						dk.toString(),
						true);
			}

			channel.sendMessage(eb.build()).queue();
			return;
		}

		try {
			int slot = Integer.parseInt(args[0]);

			if (slot < 0 || slot >= acc.getStashCapacity()) {
				channel.sendMessage("❌ | Slot inválido.").queue();
				return;
			} else if (slot == kp.getActiveDeck()) {
				channel.sendMessage("❌ | Este já é seu deck atual.").queue();
				return;
			}

			kp.setDeck(slot);
			KawaiponDAO.saveKawaipon(kp);

			channel.sendMessage("✅ | Deck alternado com sucesso.").queue();
		} catch (NumberFormatException e) {
			Deck dk = kp.getDecks().stream()
					.filter(d -> d.getName() != null)
					.filter(d -> d.getName().equalsIgnoreCase(args[0]))
					.findFirst()
					.orElse(null);
			if (dk == null) {
				channel.sendMessage("❌ | Nenhum deck com o nome `" + Helper.bugText(args[0]) + "` encontrado.").queue();
				return;
			}

			int slot = kp.getDecks().indexOf(dk);
			if (slot == kp.getActiveDeck()) {
				channel.sendMessage("❌ | Este já é seu deck atual.").queue();
				return;
			}

			kp.setDeck(slot);
			KawaiponDAO.saveKawaipon(kp);

			channel.sendMessage("✅ | Deck alternado com sucesso.").queue();
		}
	}
}
