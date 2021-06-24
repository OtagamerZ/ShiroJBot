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

package com.kuuhaku.command.commands.discord.exceed;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.ExceedScore;
import com.kuuhaku.model.records.Exceed;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "exceedrank",
		aliases = {"exrank", "topexceed", "topex"},
		usage = "req_actual",
		category = Category.EXCEED
)
@Requires({Permission.MESSAGE_ATTACH_FILES})
public class ExceedRankCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			if (ExceedDAO.getExceed(author.getId()).isBlank()) {
				m.editMessage(I18n.getString("err_exceed-rank-no-exceed")).queue();
				return;
			}

			try {
				if (args.length == 0) {
					List<Exceed> exceeds = new ArrayList<>();
					List<Color> colors = new ArrayList<>();
					for (ExceedEnum ex : ExceedEnum.values()) {
						exceeds.add(ExceedDAO.getExceed(ex));
						colors.add(ex.getPalette());
					}

					CategoryChart chart = Helper.buildBarChart(
							"Ranking dos Exceeds",
							Pair.of("", "Pontos (x1000)"),
							colors.stream().map(Color::darker).collect(Collectors.toList())
					);

					for (Exceed ex : exceeds) {
						chart.addSeries(ex.exceed().getName(),
								List.of("Exceed"),
								List.of(Math.round(ex.exp() / 1000d))
						);
					}

					channel.sendFile(Helper.writeAndGet(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "ranking", "png"))
							.queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES, null, Helper::doNothing));
					m.delete().queue();
				} else if (args[0].equalsIgnoreCase("historico")) {
					List<List<ExceedScore>> exceeds = new ArrayList<>();
					List<Color> colors = new ArrayList<>();
					for (ExceedEnum ex : ExceedEnum.values()) {
						List<ExceedScore> e = ExceedDAO.getExceedHistory(ex);
						e.add(new ExceedScore(ex, ExceedDAO.getExceed(ex).exp(), LocalDate.now().plusMonths(1)));
						e.sort(Comparator.comparing(ExceedScore::getTimestamp));
						e.removeIf(es -> es.getTimestamp().getYear() != LocalDate.now().getYear());
						exceeds.add(e);
						colors.add(ex.getPalette());
					}

					XYChart chart = Helper.buildXYChart(
							"Ranking dos Exceeds",
							Pair.of("Mês", "Pontos (x1000)"),
							colors
					);

					chart.getStyler()
							.setXAxisMin(1d)
							.setXAxisMax(12d);

					for (List<ExceedScore> ex : exceeds) {
						ExceedEnum ee = ex.get(0).getExceed();

						chart.addSeries(ee.getName(),
								ex.stream()
										.map(e -> e.getTimestamp().getMonthValue())
										.collect(Collectors.toList()),
								ex.stream()
										.map(e -> Math.round(e.getPoints() / 1000d))
										.collect(Collectors.toList())
						).setMarker(SeriesMarkers.NONE);
					}

					channel.sendFile(Helper.writeAndGet(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "ranking", "png"))
							.queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES, null, Helper::doNothing));
					m.delete().queue();
				} else {
					m.editMessage("❌ | Você precisa digitar `historico` se deseja ver o histórico de ranking, ou não digitar nada se deseja ver o ranking atual.").queue();
				}
			} catch (Exception e) {
				m.editMessage(I18n.getString("err_exceed-rank")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
