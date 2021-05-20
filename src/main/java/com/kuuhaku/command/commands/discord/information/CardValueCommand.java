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
import com.kuuhaku.model.common.MarketValue;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.OHLCChart;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "valor",
		aliases = {"value"},
		usage = "req_card",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EXT_EMOJI})
public class CardValueCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando gráfico...").queue(m -> {
			if (args.length < 1) {
				channel.sendMessage("❌ | Você precisa informar uma carta para ver o gráfico de preços.").queue();
				m.delete().queue();
				return;
			}

			Card c = CardDAO.getRawCard(args[0]);
			KawaiponRarity r = KawaiponRarity.getByName(args[0]);

			if (c == null && r == null) {
				channel.sendMessage("❌ | Essa carta ou raridade não existe.").queue();
				m.delete().queue();
				return;
			}

			List<MarketValue> values;

			if (c != null)
				values = StockMarketDAO.getCardHistory(c);
			else
				values = StockMarketDAO.getRarityHistory(r);

			OHLCChart chart = Helper.buildOHLCChart(
					"Valores de venda da " + (c == null ? "raridade" : "carta") + " \"" + (c == null ? r.toString() : c.getName()) + "\"",
					Pair.of("Data", "Valor"),
					List.of(new Color(0, 0, 0), Color.white)
			);

			if (values.size() <= 1) {
				m.editMessage("❌ | Essa carta ainda não foi vendida ou possui apenas 1 venda.").queue();
				return;
			}

			chart.addSeries("Valor",
					values.stream().map(MarketValue::getDate).collect(Collectors.toList()),
					values.stream().map(MarketValue::getOpen).collect(Collectors.toList()),
					values.stream().map(MarketValue::getHigh).collect(Collectors.toList()),
					values.stream().map(MarketValue::getLow).collect(Collectors.toList()),
					values.stream().map(MarketValue::getClose).collect(Collectors.toList())
			).setDownColor(new Color(255, 0, 0, 100))
					.setUpColor(new Color(0, 255, 0, 100));

			chart.addSeries("Valor",
					values.stream().map(MarketValue::getDate).collect(Collectors.toList()),
					values.stream().map(MarketValue::getValue).collect(Collectors.toList())
			).setMarker(SeriesMarkers.NONE);

			channel.sendFile(Helper.writeAndGet(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "chart", "png")).queue();
			m.delete().queue();
		});
	}
}
