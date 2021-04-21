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
import com.kuuhaku.controller.postgresql.StockMarketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.StocksPanel;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "bolsa",
		aliases = {"stockmarket", "stocks"},
		usage = "req_card",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS})
public class StockMarketCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendFile(Helper.getBytes(new StocksPanel().view(), "jpg"), "panel.jpg").queue();
			return;
		}

		Card c = CardDAO.getRawCard(args[0]);
		if (c == null) {
			channel.sendMessage("❌ | Essa carta não existe.").queue();
			return;
		}

		int value = StockMarketDAO.getValues().get(c.getId()).getValue();
		double growth = Math.floor(StockMarketDAO.getValues().get(c.getId()).getGrowth() * 1000) / 1000;

		String emote;
		if (growth > 0)
			emote = "<:growth:801508203480481862>";
		else if (growth < 0)
			emote = "<:noun:801508203510235186>";
		else
			emote = "<:stall:801508203384799233>";

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(c.getName())
				.setImage("attachment://card.png")
				.addField("Valor de mercado", value == 0 ? "desconhecido" : Helper.separate(Math.round(value)) + " créditos", true)
				.addField("Variação de valor", emote + (growth > 0 ? " +" : " ") + Helper.round(growth * 100, 3) + "%", true);

		channel.sendMessage(eb.build())
				.addFile(Helper.getBytes(c.drawCardNoBorder(), "png"), "card.png")
				.queue();
	}
}
