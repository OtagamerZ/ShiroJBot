/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.CardDAO;
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
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.io.File;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

		for (int i = 1; i < stats.size() - 1; i++) {
			BotStats prev = stats.get(i - 1);
			BotStats curr = stats.get(i);
			BotStats next = stats.get(i + 1);

			curr.setAverageMemory(Helper.average(prev.getMemoryUsage(), curr.getMemoryUsage(), next.getMemoryUsage()));
		}

		Map<Date, BotStats> reducedStats = new TreeMap<>(Date::compareTo);
		for (BotStats stat : stats) {
			reducedStats.put(Date.from(stat.getTimestamp().truncatedTo(ChronoUnit.MINUTES).toInstant()), stat);
		}

		channel.sendMessage("<a:loading:697879726630502401> Gerando gráficos...").queue(m -> {
			if (reducedStats.size() <= 1) {
				m.editMessage("❌ | Dados insuficientes para gerar os gráficos.").queue();
				return;
			}

			File generalStats = getGeneralStats(reducedStats);
			File cacheStats = getCacheStats(reducedStats);

			channel.sendFile(generalStats, "general.png")
					.addFile(cacheStats, "cache.png")
					.flatMap(s -> m.delete())
					.queue();
		});
	}

	private File getGeneralStats(Map<Date, BotStats> reducedStats) {
		XYChart chart = Helper.buildXYChart(
				"Estatísticas sobre a Shiro J. Bot",
				Pair.of("Data", ""),
				List.of(
						new Color(60, 177, 28),
						new Color(158, 220, 140, 26),
						new Color(158, 220, 140),
						new Color(224, 123, 46),
						new Color(36, 172, 227),
						new Color(130, 32, 243)
				)
		);

		AxesChartStyler styler = chart.getStyler()
				.setYAxisMin(1, 0d)
				.setYAxisMax(1, 100d);

		//noinspection SuspiciousNameCombination
		styler.setYAxisGroupPosition(0, Styler.YAxisPosition.Left);
		//noinspection SuspiciousNameCombination
		styler.setYAxisGroupPosition(1, Styler.YAxisPosition.Right);
		chart.getStyler().setDecimalPattern("0");

		chart.setYAxisGroupTitle(0, "Absoluto");
		chart.setYAxisGroupTitle(1, "%");

		chart.addSeries(
				"Uso de memória (%)",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(s -> Helper.round(s.getMemoryPrcnt() * 100, 1)).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(1);

		chart.addSeries(
				"Uso de memória (MB)",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(s -> StorageUnit.MB.convert(s.getMemoryUsage(), StorageUnit.B)).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		chart.addSeries(
				"Uso de memória (Avg)",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(s -> StorageUnit.MB.convert(s.getAverageMemory(), StorageUnit.B)).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		chart.addSeries(
				"Uso de CPU (%)",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(s -> Helper.round(s.getCpuUsage() * 100, 1)).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(1);

		chart.addSeries(
				"Ping (ms)",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getPing).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		chart.addSeries(
				"Servidores",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getServers).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		return Helper.writeAndGet(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "stats", "png");
	}

	private File getCacheStats(Map<Date, BotStats> reducedStats) {
		XYChart chart = Helper.buildXYChart(
				"Caching da Shiro J. Bot",
				Pair.of("Data", ""),
				List.of(
						new Color(180, 0, 0),
						new Color(180, 114, 0),
						new Color(0, 180, 159),
						new Color(27, 180, 0),
						new Color(180, 0, 162),
						new Color(111, 0, 180),
						new Color(0, 60, 180)
				)
		);

		AxesChartStyler styler = chart.getStyler()
				.setYAxisMin(1, 0d)
				.setYAxisMax(1, 100d);

		//noinspection SuspiciousNameCombination
		styler.setYAxisGroupPosition(0, Styler.YAxisPosition.Left);
		//noinspection SuspiciousNameCombination
		styler.setYAxisGroupPosition(1, Styler.YAxisPosition.Right);
		chart.getStyler().setDecimalPattern("0");

		chart.setYAxisGroupTitle(0, "Absoluto");
		chart.setYAxisGroupTitle(1, "%");

		chart.addSeries(
				"Ratelimit",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getRatelimitCount).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		chart.addSeries(
				"Confirmações",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getConfirmationPendingCount).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		chart.addSeries(
				"Eventos especiais",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getSpecialEventCount).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		chart.addSeries(
				"Drops spawnados",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getCurrentDropCount).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		chart.addSeries(
				"Cartas spawnadas",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getCurrentCardCount).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		long maxCards = CardDAO.getTotalCards();
		chart.addSeries(
				"Cartas em cache (%)",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(s -> Helper.prcnt(s.getCardCacheCount(), maxCards) * 100).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(1);

		chart.addSeries(
				"Recursos em cache",
				List.copyOf(reducedStats.keySet()),
				reducedStats.values().stream().map(BotStats::getResourceCacheCount).collect(Collectors.toList())
		).setMarker(SeriesMarkers.NONE)
				.setYAxisGroup(0);

		return Helper.writeAndGet(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "cache", "png");
	}
}
