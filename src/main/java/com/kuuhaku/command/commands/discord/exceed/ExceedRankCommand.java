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
import com.kuuhaku.utils.ExceedEnum;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.DialChart;
import org.knowm.xchart.DialChartBuilder;
import org.knowm.xchart.style.Styler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
				List<Exceed> exceeds = new ArrayList<>();
				for (ExceedEnum ex : ExceedEnum.values()) {
					exceeds.add(ExceedDAO.getExceed(ex));
				}

				DialChart chart = new DialChartBuilder()
						.width(800)
						.height(600)
						.title("Pontuação dos Exceeds")
						.build();

				chart.getStyler()
						.setLegendPosition(Styler.LegendPosition.InsideNE)
						.setHasAnnotations(true)
						.setSeriesColors(
								exceeds.stream()
										.map(Exceed::getExceed)
										.map(ExceedEnum::getPalette)
										.map(Color::brighter)
										.toArray(Color[]::new)
						);

				for (Exceed ex : exceeds) {
					chart.addSeries(ex.getExceed().getName(),
							ex.getExp(),
							"Pontuação total"
					);
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(Profile.clipRoundEdges(BitmapEncoder.getBufferedImage(chart)), "png", baos);
				channel.sendFile(baos.toByteArray(), "ranking.png").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
				m.delete().queue();
			} catch (Exception e) {
				m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-rank")).queue();
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
