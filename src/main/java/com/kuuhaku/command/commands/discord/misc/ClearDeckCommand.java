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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ShoukanDeck;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.Stash;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.ImageHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "limpardeck",
		aliases = {"ldeck", "cleardeck", "cdeck"},
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_ATTACH_FILES
})
public class ClearDeckCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (StashDAO.getRemainingSpace(author.getId()) <= 0) {
			channel.sendMessage("❌ | Seu armazém está lotado, abra espaço nele antes de limpar seu deck.").queue();
			return;
		}

		Deck dk = KawaiponDAO.getDeck(author.getId());
		ShoukanDeck sd = new ShoukanDeck(Account.find(Account.class, author.getId()));

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		eb.setTitle("Por favor confirme!");
		eb.setDescription("Seu deck será limpo e todas as cartas nele serão colocadas no armazém, por favor clique no botão abaixo para confirmar.");
		eb.setImage("attachment://deque.jpg");

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage(I18n.getString("str_generating-deck")).queue(m -> {
			File f = ImageHelper.writeAndGet(sd.view(dk), "deque", "jpg");
			channel.sendMessageEmbeds(eb.build()).addFile(f)
					.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());

								if (!dk.isNovice()) {
									for (Champion c : dk.getChampions()) {
										Stash st = new Stash(author.getId(), new KawaiponCard(c.getCard(), false));
										StashDAO.saveCard(st);
									}
								}
								dk.getChampions().clear();

								for (Evogear e : dk.getEquipments()) {
									Stash st = new Stash(author.getId(), e);
									StashDAO.saveCard(st);
								}
								dk.getEquipments().clear();

								for (Field fd : dk.getFields()) {
									Stash st = new Stash(author.getId(), fd);
									StashDAO.saveCard(st);
								}
								dk.getFields().clear();

								KawaiponDAO.saveDeck(dk);
								s.delete().queue();
								channel.sendMessage("✅ | Deck limpo com sucesso!").queue();
							}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
					));
			m.delete().queue();
		});
	}
}
