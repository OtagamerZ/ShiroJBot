/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ShoukanDeck;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.image.BufferedImage;
import java.io.IOException;

@Command(
		name = "deck",
		usage = "req_daily-meta-p-c",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS})
public class ShoukanDeckCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		boolean showPrivate = Helper.equalsAny("p", args);

		channel.sendMessage(I18n.getString("str_generating-deck")).queue(m -> {
			if (Helper.containsAny(args, "daily", "diario")) {
				try {
					Deck dk = Helper.getDailyDeck();

					ShoukanDeck kb = new ShoukanDeck(AccountDAO.getAccount(author.getId()));
					BufferedImage cards = kb.view(dk);

					EmbedBuilder eb = new ColorlessEmbedBuilder()
							.setTitle(":date: | Deck diário")
							.setDescription("O deck diário será o mesmo para todos os jogadores até amanhã, e permite que usuários que não possuam 30 cartas Senshi joguem. Ganhar usando ele premiará seu Exceed com 5x mais pontos de influência (PDI).")
							.setImage("attachment://deck.jpg");

					m.delete().queue();
					channel.sendMessage(eb.build()).addFile(Helper.writeAndGet(cards, "deck", "jpg")).queue();
				} catch (IOException e) {
					m.editMessage(I18n.getString("err_deck-generation-error")).queue();
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			} else if (Helper.containsAny(args, "meta")) {
				try {
					Deck dk = CardDAO.getMetaDeck();

					ShoukanDeck kb = new ShoukanDeck(AccountDAO.getAccount(author.getId()));
					BufferedImage cards = kb.view(dk);

					EmbedBuilder eb = new ColorlessEmbedBuilder()
							.setTitle(":date: | Deck meta")
							.setDescription("O deck meta reflete as cartas mais utilizadas pela comunidade (não necessáriamente sendo a melhor combinação possível).")
							.setImage("attachment://deck.jpg");

					m.delete().queue();
					channel.sendMessage(eb.build()).addFile(Helper.writeAndGet(cards, "deck", "jpg")).queue();
				} catch (IOException e) {
					m.editMessage(I18n.getString("err_deck-generation-error")).queue();
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			} else {
				try {
					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
					Deck dk = kp.getDeck();

					ShoukanDeck kb = new ShoukanDeck(AccountDAO.getAccount(author.getId()));
					BufferedImage cards = kb.view(dk);

					EmbedBuilder eb = new ColorlessEmbedBuilder()
							.setTitle(":beginner: | Deck de " + author.getName())
							.addField(":crossed_swords: | Cartas Senshi:", dk.getChampions().size() + " de 36", true)
							.addField(":shield: | Peso evogear:", dk.getEvoWeight() + " de 24", true)
							.setImage("attachment://deck.jpg");

					m.delete().queue();
					if (showPrivate) {
						author.openPrivateChannel()
								.flatMap(c -> c.sendMessage(eb.build()).addFile(Helper.writeAndGet(cards, "deck", "jpg")))
								.flatMap(c -> channel.sendMessage("Deck enviado nas suas mensagens privadas."))
								.queue(null, Helper::doNothing);
					} else {
						channel.sendMessage(eb.build()).addFile(Helper.writeAndGet(cards, "deck", "jpg")).queue();
					}
				} catch (IOException e) {
					m.editMessage(I18n.getString("err_deck-generation-error")).queue();
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			}
		});
	}
}