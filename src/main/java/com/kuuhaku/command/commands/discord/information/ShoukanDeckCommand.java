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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ShoukanDeck;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.awt.image.BufferedImage;
import java.io.IOException;

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
			if (Helper.containsAny(args, "daily", "diario")) {
				try {
					Kawaipon kp = Helper.getDailyDeck();

					ShoukanDeck kb = new ShoukanDeck(AccountDAO.getAccount(author.getId()));
					BufferedImage cards = kb.view(kp.getChampions(), kp.getEquipments());

					EmbedBuilder eb = new ColorlessEmbedBuilder()
							.setTitle(":date: | Deck de Diário")
							.setDescription("O deck diário será o mesmo para todos os jogadores até amanhã, e permite que usuários que não possuam 36 cartas Senshi joguem. Ganhar usando ele premiará seu Exceed com 5x mais pontos de influência (PDI).")
							.setImage("attachment://deck.jpg");

					m.delete().queue();
					channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards, "jpg", 0.5f), "deck.jpg").queue();
				} catch (IOException | InterruptedException e) {
					m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_deck-generation-error")).queue();
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			} else {
				try {
					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					ShoukanDeck kb = new ShoukanDeck(AccountDAO.getAccount(author.getId()));
					BufferedImage cards = kb.view(kp.getChampions(), kp.getEquipments());

					EmbedBuilder eb = new ColorlessEmbedBuilder()
							.setTitle(":beginner: | Deck de " + author.getName())
							.addField(":crossed_swords: | Cartas Senshi:", kp.getChampions().size() + " de 36", true)
							.addField(":shield: | Cartas EvoGear:", kp.getEquipments().size() + " de 18", true)
							.setImage("attachment://deck.jpg");

					m.delete().queue();
					channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards, "jpg", 0.5f), "deck.jpg").queue();
				} catch (IOException | InterruptedException e) {
					m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_deck-generation-error")).queue();
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			}
		});
	}
}