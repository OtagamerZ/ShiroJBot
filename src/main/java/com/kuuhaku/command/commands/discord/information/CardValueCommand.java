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
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.CardMarketDAO;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.CardMarket;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CardValueCommand extends Command {

	private static final String STR_LEVEL = "str_level";
	private static final String STR_CREDIT = "str_credit";
	private static final String STR_CARD = "str_card";
	private static final String SRT_USER_RANKING_TITLE = "str_user-ranking-title";
	private static final String STR_GLOBAL = "str_global";
	private static final String STR_LOCAL = "str_local";

	public CardValueCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public CardValueCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public CardValueCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public CardValueCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando gráfico...").queue(m -> {
			if (args.length < 1) {
				channel.sendMessage("❌ | Você precisa informar uma carta para ver o gráfico de preços.").queue();
				return;
			}

			Card c = CardDAO.getCard(args[0], false);

			if (c == null) {
				channel.sendMessage("❌ | Essa carta não existe.").queue();
				return;
			}

			List<CardMarket> normalCards = CardMarketDAO.getCardsByCard(c.getId(), false);
			List<CardMarket> foilCards = CardMarketDAO.getCardsByCard(c.getId(), true);

			XYChart chart = new XYChartBuilder()
					.width(800)
					.height(600)
					.title("Valores de venda da carta \"" + c.getName() + "\"")
					.xAxisTitle("ID")
					.yAxisTitle("Valor")
					.build();

			chart.getStyler()
					.setPlotGridLinesColor(Color.decode("#404447"))
					.setXAxisMin(1d)
					.setXAxisMax(12d)
					.setAxisTickLabelsColor(Color.WHITE)
					.setChartFontColor(Color.WHITE)
					.setLegendPosition(Styler.LegendPosition.InsideNE)
					.setSeriesColors(new Color[]{Color.gray, Color.yellow})
					.setPlotBackgroundColor(Color.decode("#202225"))
					.setChartBackgroundColor(Color.decode("#101114"))
					.setLegendBackgroundColor(Color.decode("#101114"))
					.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

			chart.addSeries("Normal",
					normalCards.stream()
							.map(CardMarket::getId)
							.collect(Collectors.toList()),
					normalCards.stream()
							.map(CardMarket::getPrice)
							.collect(Collectors.toList())
			);

			chart.addSeries("Cromada",
					foilCards.stream()
							.map(CardMarket::getId)
							.collect(Collectors.toList()),
					foilCards.stream()
							.map(CardMarket::getPrice)
							.collect(Collectors.toList())
			);

			channel.sendFile(Helper.getBytes(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png"), "ranking.png").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
			m.delete().queue();
		});
	}
}
