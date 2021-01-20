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
import com.kuuhaku.controller.postgresql.EquipmentMarketDAO;
import com.kuuhaku.controller.postgresql.FieldMarketDAO;
import com.kuuhaku.model.common.Market;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CardValueCommand extends Command {

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

			List<Market> normalCards;
			List<Market> foilCards;

			if (c != null) {
				normalCards = Stream.of(
						CardMarketDAO.getCardsByCard(c.getId(), false),
						EquipmentMarketDAO.getCardsByCard(c.getId()),
						FieldMarketDAO.getCardsByCard(c.getId())
				).flatMap(List::stream)
						.filter(cm -> cm.getPublishDate().after(Date.from(Instant.now().minus(30, ChronoUnit.DAYS))))
						.collect(Collectors.toList());
				foilCards = CardMarketDAO.getCardsByCard(c.getId(), true)
						.stream()
						.filter(cm -> cm.getPublishDate().after(Date.from(Instant.now().minus(30, ChronoUnit.DAYS))))
						.collect(Collectors.toList());
			} else {
				normalCards = CardMarketDAO.getCardsByRarity(r, false)
						.stream()
						.filter(cm -> cm.getPublishDate().after(Date.from(Instant.now().minus(30, ChronoUnit.DAYS))))
						.collect(Collectors.toList());
				foilCards = CardMarketDAO.getCardsByRarity(r, true)
						.stream()
						.filter(cm -> cm.getPublishDate().after(Date.from(Instant.now().minus(30, ChronoUnit.DAYS))))
						.collect(Collectors.toList());
			}

			XYChart chart = new XYChartBuilder()
					.width(800)
					.height(600)
					.title("Valores de venda da " + (c == null ? "raridade" : "carta") + " \"" + (c == null ? r.toString() : c.getName()) + "\"")
					.yAxisTitle("Valor (x1000)")
					.xAxisTitle("Data")
					.build();

			chart.getStyler()
					.setPlotGridLinesColor(new Color(64, 68, 71))
					.setAxisTickLabelsColor(Color.WHITE)
					.setChartFontColor(Color.WHITE)
					.setLegendPosition(Styler.LegendPosition.InsideNE)
					.setSeriesColors(new Color[]{new Color(0, 150, 0), Color.yellow})
					.setPlotBackgroundColor(new Color(32, 34, 37))
					.setChartBackgroundColor(new Color(16, 17, 20))
					.setLegendBackgroundColor(new Color(16, 17, 20, 100))
					.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

			Map<Date, Integer> normalValues = new HashMap<>();
			for (Market nc : normalCards) {
				if (normalValues.containsKey(nc.getPublishDate()))
					normalValues.computeIfPresent(nc.getPublishDate(), (k, v) -> Math.round(v + nc.getPrice() / 2f));
				else
					normalValues.put(nc.getPublishDate(), nc.getPrice());
			}

			Map<Date, Integer> foilValues = new HashMap<>();
			for (Market fc : foilCards) {
				if (foilValues.containsKey(fc.getPublishDate()))
					foilValues.computeIfPresent(fc.getPublishDate(), (k, v) -> Math.round(v + fc.getPrice() / 2f));
				else
					foilValues.put(fc.getPublishDate(), fc.getPrice());
			}

			List<Map.Entry<Date, Integer>> normalData = normalValues.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
			List<Map.Entry<Date, Integer>> foilData = foilValues.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());

			if (normalData.size() <= 1 && foilData.size() <= 1) {
				channel.sendMessage("❌ | Essa carta ainda não foi vendida nos últimos 30 dias ou possui vendas em apenas .").queue();
				m.delete().queue();
				return;
			}

			if (normalCards.size() > 1)
				chart.addSeries("Normal",
						normalData.stream()
								.map(Map.Entry::getKey)
								.collect(Collectors.toList()),
						normalData.stream()
								.map(Map.Entry::getValue)
								.map(v -> Helper.round(v / 1000d, 1))
								.collect(Collectors.toList())
				);

			if (foilCards.size() > 1)
				chart.addSeries("Cromada",
						foilData.stream()
								.map(Map.Entry::getKey)
								.collect(Collectors.toList()),
						foilData.stream()
								.map(Map.Entry::getValue)
								.map(v -> Helper.round(v / 1000d, 1))
								.collect(Collectors.toList())
				);

			channel.sendFile(Helper.getBytes(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png"), "ranking.png").queue();
			m.delete().queue();
		});
	}
}
