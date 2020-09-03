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
import com.kuuhaku.model.persistent.ExceedScore;
import com.kuuhaku.utils.ExceedEnum;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			if (ExceedDAO.getExceed(author.getId()).isBlank()) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-rank-no-exceed")).queue();
				return;
			}

			try {
				if (args.length == 0) {
					List<List<ExceedScore>> exceeds = new ArrayList<>();
					List<Color> colors = new ArrayList<>();
					for (ExceedEnum ex : ExceedEnum.values()) {
						exceeds.add(ExceedDAO.getExceedHistory(ex));
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
							.setPlotGridLinesColor(Color.decode("#404447"))
							.setXAxisMin(1d)
							.setXAxisMax(12d)
							.setAxisTickLabelsColor(Color.WHITE)
							.setAnnotationsFontColor(Color.WHITE)
							.setChartFontColor(Color.WHITE)
							.setLegendPosition(Styler.LegendPosition.InsideNE)
							.setSeriesColors(colors.toArray(Color[]::new))
							.setPlotBackgroundColor(Color.decode("#202225"))
							.setChartBackgroundColor(Color.decode("#101114"))
							.setLegendBackgroundColor(Color.decode("#101114"))
							.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

					for (List<ExceedScore> ex : exceeds) {
						ExceedEnum ee = ex.get(0).getExceed();
						ex.add(new ExceedScore(ee, ExceedDAO.getExceed(ee).getExp(), LocalDate.now()));

						chart.addSeries(ee.getName(),
								ex.stream()
										.map(e -> e.getTimestamp().getMonthValue())
										.collect(Collectors.toList()),
								ex.stream()
										.map(e -> Math.round(e.getPoints() / 1000d))
										.collect(Collectors.toList())
						);
					}

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png", baos);
					channel.sendFile(baos.toByteArray(), "ranking.png").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
					m.delete().queue();
				} else if (args[0].equalsIgnoreCase("atual")) {
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
							.setAxisTickLabelsColor(Color.WHITE)
							.setAnnotationsFontColor(Color.WHITE)
							.setChartFontColor(Color.WHITE)
							.setLegendPosition(Styler.LegendPosition.InsideNE)
							.setSeriesColors(colors.toArray(Color[]::new))
							.setPlotBackgroundColor(Color.decode("#202225"))
							.setChartBackgroundColor(Color.decode("#101114"))
							.setLegendBackgroundColor(Color.decode("#101114"));

					for (Exceed ex : exceeds) {
						chart.addSeries(ex.getExceed().getName(),
								List.of("Pontos Totais"),
								List.of(Math.round(ex.getExp() / 1000d))
						);
					}

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png", baos);
					channel.sendFile(baos.toByteArray(), "ranking.png").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
					m.delete().queue();
				}
			} catch (Exception e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-rank")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
