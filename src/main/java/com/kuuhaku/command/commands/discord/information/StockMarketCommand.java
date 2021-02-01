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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.CardMarketDAO;
import com.kuuhaku.controller.postgresql.EquipmentMarketDAO;
import com.kuuhaku.controller.postgresql.FieldMarketDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.stream.DoubleStream;


public class StockMarketCommand implements Executable {

	public StockMarketCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public StockMarketCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public StockMarketCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public StockMarketCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar uma carta.").queue();
			return;
		}

		Card c = CardDAO.getRawCard(args[0]);
		if (c == null) {
			channel.sendMessage("❌ | Essa carta não existe.").queue();
			return;
		}

		double stock = DoubleStream.of(
				CardMarketDAO.getStockValue(c),
				EquipmentMarketDAO.getStockValue(c),
				FieldMarketDAO.getStockValue(c)
		).filter(d -> d > 0).average().orElse(0);

		double current = DoubleStream.of(
				CardMarketDAO.getAverageValue(c),
				EquipmentMarketDAO.getAverageValue(c),
				FieldMarketDAO.getAverageValue(c)
		).filter(d -> d > 0).average().orElse(0);

		String emote;
		if (stock > 0)
			emote = "<:growth:801508203480481862>";
		else if (stock < 0)
			emote = "<:noun:801508203510235186>";
		else
			emote = "<:stall:801508203384799233>";

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(c.getName())
				.setImage("attachment://card.png")
				.addField("Valor de mercado", current == 0 ? "desconhecido" : Math.round(current) + " créditos", true)
				.addField("Variação de valor", emote + (stock > 0 ? " +" : " ") + (Helper.round(stock * 100, 3)) + "%", true);

		channel.sendMessage(eb.build())
				.addFile(Helper.getBytes(c.drawCardNoBorder(), "png"), "card.png")
				.queue();
	}
}
