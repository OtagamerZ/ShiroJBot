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
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.utils.Helper;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "card")
public class Card {
	@Id
	private String id;

	@Column(columnDefinition = "VARCHAR(32) NOT NULL DEFAULT ''")
	private String name = "";

	@ManyToOne(fetch = FetchType.EAGER)
	private AddedAnime anime;

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

	public AddedAnime getAnime() {
		return anime;
	}

	public void setAnime(AddedAnime anime) {
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
			byte[] cardBytes = Main.getInfo().getCardCache().computeIfAbsent(id + (foil ? "_F" : "_N"), k -> {
				try {
					return FileUtils.readFileToByteArray(new File(System.getenv("CARDS_PATH") + anime.getName(), id + ".png"));
				} catch (IOException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
					return null;
				}
			});

			assert cardBytes != null;
			try (ByteArrayInputStream bais = new ByteArrayInputStream(cardBytes)) {
				BufferedImage card = ImageIO.read(bais);

				BufferedImage frame = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/frames/new/" + rarity.name().toLowerCase(Locale.ROOT) + ".png")));
				BufferedImage canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = canvas.createGraphics();

				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.drawImage(foil ? adjust(card, false) : card, 15, 15, null);
				g2d.drawImage(foil ? adjust(frame, true) : frame, 0, 0, null);

				g2d.dispose();

				return canvas;
			}
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	public BufferedImage drawCardNoBorder() {
		try {
			byte[] cardBytes = Main.getInfo().getCardCache().computeIfAbsent(id, k -> {
				try {
					return FileUtils.readFileToByteArray(new File(System.getenv("CARDS_PATH") + anime.getName(), id + ".png"));
				} catch (IOException e) {
					return null;
				}
			});

			assert cardBytes != null;
			try (ByteArrayInputStream bais = new ByteArrayInputStream(cardBytes)) {
				return ImageIO.read(bais);
			}
		} catch (IOException e) {
			return null;
		}
	}

	public BufferedImage drawCardNoBorder(boolean foil) {
		try {
			byte[] cardBytes = Main.getInfo().getCardCache().computeIfAbsent(id, k -> {
				try {
					return FileUtils.readFileToByteArray(new File(System.getenv("CARDS_PATH") + anime.getName(), id + ".png"));
				} catch (IOException e) {
					return null;
				}
			});

			assert cardBytes != null;
			try (ByteArrayInputStream bais = new ByteArrayInputStream(cardBytes)) {
				return foil ? adjust(ImageIO.read(bais), false) : ImageIO.read(bais);
			}
		} catch (IOException e) {
			return null;
		}
	}

	private BufferedImage adjust(BufferedImage bi, boolean border) {
		BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < bi.getHeight(); y++) {
			for (int x = 0; x < bi.getWidth(); x++) {
				int[] rgb = Helper.unpackRGB(bi.getRGB(x, y));
				int alpha = rgb[0];
				float[] hsv;
				if (border) {
					hsv = Color.RGBtoHSB(rgb[1], rgb[2], rgb[3], null);
				} else {
					hsv = Color.RGBtoHSB(rgb[1], rgb[3], rgb[2], null);
				}

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

	@Override
	public String toString() {
		return new JSONObject() {{
			put("id", id);
			put("name", name);
			if (!anime.getName().equals("HIDDEN"))
				put("anime", new JSONObject() {{
					put("id", anime.getName());
					put("name", anime.toString());
				}});
			put("rarity", rarity.getIndex());
		}}.toString();
	}

	public String getBase64() {
		return Helper.atob(drawCard(false), "png");
	}
}
