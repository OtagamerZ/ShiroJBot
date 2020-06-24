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

import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import org.apache.commons.io.IOUtils;
import org.jdesktop.swingx.graphics.BlendComposite;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CardType extends Card {
	private boolean foil;

	public boolean isFoil() {
		return foil;
	}

	public void setFoil(boolean foil) {
		this.foil = foil;
	}

	public BufferedImage getCard() {
		try {
			byte[] cardBytes = ShiroInfo.getCardCache().get(getImgurId(), () -> IOUtils.toByteArray(Helper.getImage("https://i.imgur.com/" + getImgurId() + ".jpg")));
			try (ByteArrayInputStream bais = new ByteArrayInputStream(cardBytes)) {
				BufferedImage card = ImageIO.read(bais);

				BufferedImage frame = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/frames/" + getRarity().name().toLowerCase() + ".png")));
				BufferedImage canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);

				Graphics2D g2d = canvas.createGraphics();
				g2d.drawImage(card, 10, 10, 225, 350, null);

				if (foil) {
					g2d.setComposite(BlendComposite.Hue);
					g2d.drawImage(invert(card), 10, 10, 225, 350, null);
					g2d.dispose();

					g2d = canvas.createGraphics();
				}

				g2d.drawImage(frame, 0, 0, null);

				g2d.dispose();

				return canvas;
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
				Color col = new Color(rgb);
				col = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
				out.setRGB(x, y, col.getRGB());
			}
		}

		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CardType card = (CardType) o;
		return Objects.equals(getId(), card.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
