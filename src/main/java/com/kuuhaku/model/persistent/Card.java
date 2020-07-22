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

import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.KawaiponRarity;
import com.kuuhaku.utils.ShiroInfo;
import org.apache.commons.io.IOUtils;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.jdesktop.swingx.graphics.ColorUtilities;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Entity
@Table(name = "card")
public class Card {
	@Id
	private String id;

	@Column(columnDefinition = "VARCHAR(18) NOT NULL DEFAULT ''")
	private String name = "";

	@Enumerated(EnumType.STRING)
	private AnimeName anime;

	@Enumerated(EnumType.STRING)
	private KawaiponRarity rarity = KawaiponRarity.COMMON;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String imgurId = "";

	public String getName() {
		return name;
	}

	public AnimeName getAnime() {
		return anime;
	}

	public KawaiponRarity getRarity() {
		return rarity;
	}

	public BufferedImage drawCard(boolean foil) {
		try {
			byte[] cardBytes = ShiroInfo.getCardCache().get(imgurId, () -> IOUtils.toByteArray(Helper.getImage("https://i.imgur.com/" + imgurId + ".jpg")));
			System.out.println("https://i.imgur.com/" + imgurId + ".jpg");
			try (ByteArrayInputStream bais = new ByteArrayInputStream(cardBytes)) {
				BufferedImage card = ImageIO.read(bais);

				BufferedImage frame = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/frames/" + rarity.name().toLowerCase() + ".png")));
				BufferedImage frameCanvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);

				Graphics2D g2d = frameCanvas.createGraphics();
				g2d.drawImage(frame, 0, 0, null);

				if (foil) {
					g2d.setComposite(BlendComposite.Hue);
					g2d.drawImage(invert(frame), 0, 0, null);
					g2d.setComposite(AlphaComposite.SrcOver);
				}

				g2d.dispose();

				BufferedImage cardCanvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);

				g2d = cardCanvas.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.drawImage(card, 10, 10, 225, 350, null);

				if (foil) {
					g2d.setComposite(BlendComposite.Hue);
					g2d.drawImage(adjust(card), 10, 10, 225, 350, null);
					g2d.setComposite(AlphaComposite.SrcOver);
				}
				g2d.drawImage(frameCanvas, 0, 0, null);

				g2d.dispose();

				return cardCanvas;
			}
		} catch (IOException | ExecutionException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	private BufferedImage invert(BufferedImage bi) {
		BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int x = 0; x < bi.getWidth(); x++) {
			for (int y = 0; y < bi.getHeight(); y++) {
				int rgb = bi.getRGB(x, y);
				Color col = new Color(rgb, true);
				col = new Color(255 - col.getBlue(), 255 - col.getGreen(), 255 - col.getRed(), col.getAlpha());
				out.setRGB(x, y, col.getRGB());
			}
		}

		return out;
	}

	private BufferedImage adjust(BufferedImage bi) {
		BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int x = 0; x < bi.getWidth(); x++) {
			for (int y = 0; y < bi.getHeight(); y++) {
				int rgb = bi.getRGB(x, y);
				Color col = new Color(rgb, true);
				col = new Color(col.getRed(), col.getBlue(), col.getGreen());
				float[] hsv = ColorUtilities.RGBtoHSL(col);
				hsv[0] = ((hsv[0] * 255 + 30) % 255) / 255;

				out.setRGB(x, y, ColorUtilities.HSLtoRGB(hsv[0], hsv[1], hsv[2]).getRGB());
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
