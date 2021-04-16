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
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.Market;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

			List<Market> normalCards;
			List<Market> foilCards;

			ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
			if (c != null) {
				normalCards = Stream.of(
						CardMarketDAO.getCardsByCard(c.getId(), false),
						EquipmentMarketDAO.getCardsByCard(c.getId()),
						FieldMarketDAO.getCardsByCard(c.getId())
				).flatMap(List::stream)
						.filter(cm -> cm.getPublishDate().isAfter(today.minusDays(30)))
						.collect(Collectors.toList());
				foilCards = CardMarketDAO.getCardsByCard(c.getId(), true)
						.stream()
						.filter(cm -> cm.getPublishDate().isAfter(today.minusDays(30)))
						.collect(Collectors.toList());
			} else {
				normalCards = CardMarketDAO.getCardsByRarity(r, false)
						.stream()
						.filter(cm -> cm.getPublishDate().isAfter(today.minusDays(30)))
						.collect(Collectors.toList());
				foilCards = CardMarketDAO.getCardsByRarity(r, true)
						.stream()
						.filter(cm -> cm.getPublishDate().isAfter(today.minusDays(30)))
						.collect(Collectors.toList());
			}

			XYChart chart = Helper.buildXYChart(
					"Valores de venda da " + (c == null ? "raridade" : "carta") + " \"" + (c == null ? r.toString() : c.getName()) + "\"",
					Pair.of("Data", "Valor (x1000)"),
					List.of(new Color(0, 150, 0), Color.yellow)
			);

			Map<Date, Integer> normalValues = new TreeMap<>(Date::compareTo);
			for (Market nc : normalCards)
				normalValues.merge(Date.from(nc.getPublishDate().toInstant()), Math.round(nc.getPrice() / 1000f), Helper::average);

			Map<Date, Integer> foilValues = new TreeMap<>(Date::compareTo);
			for (Market fc : foilCards)
				foilValues.merge(Date.from(fc.getPublishDate().toInstant()), Math.round(fc.getPrice() / 1000f), Helper::average);

			if (normalValues.size() <= 1 && foilValues.size() <= 1) {
				m.editMessage("❌ | Essa carta ainda não foi vendida nos últimos 30 dias ou possui apenas 1 venda.").queue();
				return;
			}

			if (normalCards.size() > 1)
				chart.addSeries("Normal",
						List.copyOf(normalValues.keySet()),
						List.copyOf(normalValues.values())
				).setMarker(SeriesMarkers.NONE);

			if (foilCards.size() > 1)
				chart.addSeries("Cromada",
						List.copyOf(foilValues.keySet()),
						List.copyOf(foilValues.values())
				).setMarker(SeriesMarkers.NONE);

			channel.sendFile(Helper.getBytes(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png"), "chart.png").queue();
			m.delete().queue();
		});
	}
}
