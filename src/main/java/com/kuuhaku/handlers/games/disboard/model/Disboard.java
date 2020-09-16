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

package com.kuuhaku.handlers.games.disboard.model;

import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.enums.Country;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class Disboard {
	public static void view(TextChannel chn) {
		chn.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-map")).queue(s -> {
			try {
				List<PoliticalState> state = PStateDAO.getAllPoliticalState();
				BufferedImage ocean = getImage("layers/oceando.png");
				BufferedImage borders = getImage("layers/outline.png");

				Graphics2D g2d = ocean.createGraphics();
				for (Country c : Country.values()) {
					g2d.drawImage(paintRuler(c, state), c.getCoords().x, c.getCoords().y, null);
				}

				g2d.drawImage(borders, 0, 0, null);

				g2d.dispose();
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(ocean, "png", baos);

					chn.sendFile(baos.toByteArray(), "map.png").queue();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			s.delete().queue();
		});
	}

	private static BufferedImage getImage(String path) throws IOException {
		return ImageIO.read(Objects.requireNonNull(Disboard.class.getClassLoader().getResource(path)));
	}

	private static BufferedImage paintRuler(Country c, List<PoliticalState> state) throws IOException {
		BufferedImage country = getImage("countries/" + c.getFilepath());
		Graphics2D g2d = country.createGraphics();
		state.stream()
				.filter(ps -> ps.getCountries().contains(c.name()))
				.findFirst()
				.ifPresent(ps -> {
					g2d.setColor(ps.getExceed().getPalette());
					g2d.setComposite(AlphaComposite.SrcIn);
					g2d.fillRect(0, 0, country.getWidth(), country.getHeight());
				});
		return country;
	}
}
