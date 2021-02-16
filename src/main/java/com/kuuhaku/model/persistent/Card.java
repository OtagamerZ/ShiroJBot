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

package com.kuuhaku.model.persistent;

import com.kuuhaku.Main;
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.utils.Helper;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Entity
@Table(name = "card")
public class Card {
	@Id
	private String id;

	@Column(columnDefinition = "VARCHAR(32) NOT NULL DEFAULT ''")
	private String name = "";

	@Enumerated(EnumType.STRING)
	private AnimeName anime;

	@Enumerated(EnumType.STRING)
	private KawaiponRarity rarity = KawaiponRarity.COMMON;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AnimeName getAnime() {
		return anime;
	}

	public void setAnime(AnimeName anime) {
		this.anime = anime;
	}

	public KawaiponRarity getRarity() {
		return rarity;
	}

	public void setRarity(KawaiponRarity rarity) {
		this.rarity = rarity;
	}

	public BufferedImage drawCard(boolean foil) {
		try {
			byte[] cardBytes = Main.getInfo().getCardCache().get(id, () -> FileUtils.readFileToByteArray(new File(System.getenv("CARDS_PATH") + anime.name(), id + ".png")));
			try (ByteArrayInputStream bais = new ByteArrayInputStream(cardBytes)) {
				BufferedImage card = ImageIO.read(bais);

				BufferedImage frame = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/frames/" + rarity.name().toLowerCase() + ".png")));
				BufferedImage canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = canvas.createGraphics();

				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.drawImage(card, 10, 10, null);
				g2d.drawImage(frame, 0, 0, null);

				g2d.dispose();

				return foil ? adjust(canvas) : canvas;
			}
		} catch (IOException | ExecutionException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	public BufferedImage drawCardNoBorder() {
		try {
			byte[] cardBytes = Main.getInfo().getCardCache().get(id, () -> FileUtils.readFileToByteArray(new File(System.getenv("CARDS_PATH") + anime.name(), id + ".png")));
			try (ByteArrayInputStream bais = new ByteArrayInputStream(cardBytes)) {
				return ImageIO.read(bais);
			}
		} catch (IOException | ExecutionException e) {
			return null;
		}
	}

	private BufferedImage adjust(BufferedImage bi) {
		BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < bi.getHeight(); y++) {
			for (int x = 0; x < bi.getWidth(); x++) {
				int[] rgb = Helper.unpackRGB(bi.getRGB(x, y));
				int alpha = rgb[0];

				float[] hsv = Color.RGBtoHSB(rgb[1], rgb[3], rgb[2], null);
				hsv[0] = ((hsv[0] * 255 + 30) % 255) / 255;

				rgb = Helper.unpackRGB(Color.getHSBColor(hsv[0], hsv[1], hsv[2]).getRGB());

				out.setRGB(x, y, Helper.packRGB(alpha, rgb[1], rgb[2], rgb[3]));
			}
		}

		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Card card = (Card) o;
		return Objects.equals(id, card.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
