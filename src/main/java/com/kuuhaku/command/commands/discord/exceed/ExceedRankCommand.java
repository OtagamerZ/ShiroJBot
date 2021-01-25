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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.common.Exceed;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.ExceedScore;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ExceedRankCommand extends Command {

	public ExceedRankCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ExceedRankCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ExceedRankCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ExceedRankCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			if (ExceedDAO.getExceed(author.getId()).isBlank()) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-rank-no-exceed")).queue();
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

					CategoryChart chart = new CategoryChartBuilder()
							.width(800)
							.height(600)
							.title("Ranking dos Exceeds")
							.yAxisTitle("Pontos (x1000)")
							.build();

					chart.getStyler()
							.setPlotGridLinesColor(new Color(64, 68, 71))
							.setAxisTickLabelsColor(Color.WHITE)
							.setAnnotationsFontColor(Color.WHITE)
							.setChartFontColor(Color.WHITE)
							.setHasAnnotations(true)
							.setLegendPosition(Styler.LegendPosition.InsideNE)
							.setSeriesColors(colors.stream().map(Color::darker).toArray(Color[]::new))
							.setPlotBackgroundColor(new Color(32, 34, 37))
							.setChartBackgroundColor(new Color(16, 17, 20))
							.setLegendBackgroundColor(new Color(16, 17, 20, 100));

					for (Exceed ex : exceeds) {
						chart.addSeries(ex.getExceed().getName(),
								List.of("Exceed"),
								List.of(Math.round(ex.getExp() / 1000d))
						);
					}

					channel.sendFile(Helper.getBytes(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png"), "ranking.png").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES, null, Helper::doNothing));
					m.delete().queue();
				} else if (args[0].equalsIgnoreCase("historico")) {
					List<List<ExceedScore>> exceeds = new ArrayList<>();
					List<Color> colors = new ArrayList<>();
					for (ExceedEnum ex : ExceedEnum.values()) {
						List<ExceedScore> e = ExceedDAO.getExceedHistory(ex);
						e.add(new ExceedScore(ex, ExceedDAO.getExceed(ex).getExp(), LocalDate.now().plusMonths(1)));
						e.sort(Comparator.comparing(ExceedScore::getTimestamp));
						e.removeIf(es -> es.getTimestamp().getYear() != LocalDate.now().getYear());
						exceeds.add(e);
						colors.add(ex.getPalette());
					}

					XYChart chart = new XYChartBuilder()
							.width(800)
							.height(600)
							.title("Ranking dos Exceeds")
							.xAxisTitle("Mês")
							.yAxisTitle("Pontos (x1000)")
							.build();


					chart.getStyler()
							.setPlotGridLinesColor(new Color(64, 68, 71))
							.setXAxisMin(1d)
							.setXAxisMax(12d)
							.setAxisTickLabelsColor(Color.WHITE)
							.setChartFontColor(Color.WHITE)
							.setLegendPosition(Styler.LegendPosition.InsideNE)
							.setSeriesColors(colors.stream().map(Color::darker).toArray(Color[]::new))
							.setPlotBackgroundColor(new Color(32, 34, 37))
							.setChartBackgroundColor(new Color(16, 17, 20))
							.setLegendBackgroundColor(new Color(16, 17, 20, 100))
							.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

					for (List<ExceedScore> ex : exceeds) {
						ExceedEnum ee = ex.get(0).getExceed();

						chart.addSeries(ee.getName(),
								ex.stream()
										.map(e -> e.getTimestamp().getMonthValue())
										.collect(Collectors.toList()),
								ex.stream()
										.map(e -> Math.round(e.getPoints() / 1000d))
										.collect(Collectors.toList())
						);
					}

					channel.sendFile(Helper.getBytes(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png"), "ranking.png").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES, null, Helper::doNothing));
					m.delete().queue();
				} else {
					m.editMessage("❌ | Você precisa digitar `historico` se deseja ver o histórico de ranking, ou não digitar nada se deseja ver o ranking atual.").queue();
				}
			} catch (Exception e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-rank")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
