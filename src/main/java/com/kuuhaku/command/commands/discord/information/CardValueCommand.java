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
import com.kuuhaku.model.enums.KawaiponRarity;
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

			Card c = CardDAO.getCard(args[0], false);
			KawaiponRarity r = KawaiponRarity.getByName(args[0]);

			if (c == null && r == null) {
				channel.sendMessage("❌ | Essa carta ou raridade não existe.").queue();
				m.delete().queue();
				return;
			}

			List<CardMarket> normalCards;
			List<CardMarket> foilCards;

			if (c != null) {
				normalCards = CardMarketDAO.getCardsByCard(c.getId(), false).stream().filter(cm -> cm.getPrice() <= (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 50)).collect(Collectors.toList());
				foilCards = CardMarketDAO.getCardsByCard(c.getId(), true).stream().filter(cm -> cm.getPrice() <= (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 100)).collect(Collectors.toList());
			} else {
				normalCards = CardMarketDAO.getCardsByRarity(r, false).stream().filter(cm -> cm.getPrice() <= (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 50)).collect(Collectors.toList());
				foilCards = CardMarketDAO.getCardsByRarity(r, true).stream().filter(cm -> cm.getPrice() <= (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 100)).collect(Collectors.toList());
			}

			if (normalCards.size() <= 1 && foilCards.size() <= 1) {
				channel.sendMessage("❌ | Essa carta ainda não foi anunciada no mercado ainda ou possui apenas 1 oferta.").queue();
				m.delete().queue();
				return;
			}

			XYChart chart = new XYChartBuilder()
					.width(800)
					.height(600)
					.title("Valores de venda da " + (c == null ? "raridade" : "carta") + " \"" + (c == null ? r.toString() : c.getName()) + "\"")
					.yAxisTitle("Valor")
					.build();

			chart.getStyler()
					.setPlotGridLinesColor(Color.decode("#404447"))
					.setAxisTickLabelsColor(Color.WHITE)
					.setXAxisTicksVisible(false)
					.setChartFontColor(Color.WHITE)
					.setLegendPosition(Styler.LegendPosition.InsideNE)
					.setSeriesColors(new Color[]{new Color(0, 150, 0), Color.yellow})
					.setPlotBackgroundColor(Color.decode("#202225"))
					.setChartBackgroundColor(Color.decode("#101114"))
					.setLegendBackgroundColor(Color.decode("#101114"))
					.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

			if (normalCards.size() > 1)
				chart.addSeries("Normal",
						normalCards.stream()
								.map(CardMarket::getPublishDate)
								.collect(Collectors.toList()),
						normalCards.stream()
								.map(CardMarket::getPrice)
								.collect(Collectors.toList())
				);

			if (foilCards.size() > 1)
				chart.addSeries("Cromada",
						foilCards.stream()
								.map(CardMarket::getPublishDate)
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
