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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.BotStatsDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.StorageUnit;
import com.kuuhaku.model.persistent.BotStats;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.AxesChartStyler;
import org.knowm.xchart.style.Styler;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "status",
		aliases = {"stats"},
		category = Category.DEV
)
@Requires({Permission.MESSAGE_ATTACH_FILES})
public class BotStatsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<BotStats> stats = BotStatsDAO.getStats();

		channel.sendMessage("<a:loading:697879726630502401> Gerando gráfico...").queue(m -> {
			XYChart chart = Helper.buildXYChart(
					"Estatísticas sobre a Shiro J. Bot",
					Pair.of("Data", ""),
					List.of(
							Helper.getRandomColor(1),
							Helper.getRandomColor(2),
							Helper.getRandomColor(3),
							Helper.getRandomColor(4),
							Helper.getRandomColor(5)
					)
			);

			AxesChartStyler styler = chart.getStyler()
					.setYAxisMin(1, 0d)
					.setYAxisMax(1, 100d);

			//noinspection SuspiciousNameCombination
			styler.setYAxisGroupPosition(0, Styler.YAxisPosition.Left);
			//noinspection SuspiciousNameCombination
			styler.setYAxisGroupPosition(1, Styler.YAxisPosition.Right);

			chart.setYAxisGroupTitle(0, "Absoluto");
			chart.setYAxisGroupTitle(1, "%");

			List<Date> dates = stats.stream()
					.map(s -> Date.from(s.getTimestamp().toInstant()))
					.collect(Collectors.toList());

			chart.addSeries(
					"Uso de memória (%)",
					dates,
					stats.stream().map(s -> Helper.round(s.getMemoryPrcnt() * 100, 1)).collect(Collectors.toList())
			).setYAxisGroup(1);

			chart.addSeries(
					"Uso de CPU (%)",
					dates,
					stats.stream().map(s -> Helper.round(s.getCpuUsage() * 100, 1)).collect(Collectors.toList())
			).setYAxisGroup(1);

			chart.addSeries(
					"Uso de memória (MB)",
					dates,
					stats.stream().map(s -> StorageUnit.MB.convert(s.getMemoryUsage(), StorageUnit.B)).collect(Collectors.toList())
			).setYAxisGroup(0);

			chart.addSeries(
					"Ping (ms)",
					dates,
					stats.stream().map(BotStats::getPing).collect(Collectors.toList())
			).setYAxisGroup(0);

			chart.addSeries(
					"Servidores",
					dates,
					stats.stream().map(BotStats::getServers).collect(Collectors.toList())
			).setYAxisGroup(0);

			channel.sendFile(Helper.getBytes(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png"), "chart.png").queue();
			m.delete().queue();
		});
	}
}
