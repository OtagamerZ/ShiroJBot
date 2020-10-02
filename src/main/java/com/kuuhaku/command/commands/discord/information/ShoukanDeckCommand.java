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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ShoukanDeck;
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ShoukanDeckCommand extends Command {

	public ShoukanDeckCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ShoukanDeckCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ShoukanDeckCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ShoukanDeckCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-deck")).queue(m -> {
			try {
				Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

				Set<KawaiponCard> collection = new HashSet<>();
				for (AnimeName anime : AnimeName.validValues()) {
					if (CardDAO.totalCards(anime) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime) && !k.isFoil()).count())
						collection.add(new KawaiponCard(CardDAO.getUltimate(anime), false));
				}

				ShoukanDeck kb = new ShoukanDeck(AccountDAO.getAccount(author.getId()));
				BufferedImage cards = kb.view(kp.getChampions(), kp.getEquipments());

				EmbedBuilder eb = new ColorlessEmbedBuilder();
				eb.setTitle("\uD83D\uDD30 | Deck de " + author.getName());
				eb.addField(":crossed_swords: | Cartas Senshi:", kp.getChampions().size() + " de 40", true);
				eb.addField(":shield: | Cartas EvoGear:", kp.getEquipments().size() + " de 18", true);
				eb.setImage("attachment://deck.png");

				m.delete().queue();
				channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards, "png"), "deck.png").queue();
			} catch (IOException | InterruptedException e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_deck-generation-error")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}