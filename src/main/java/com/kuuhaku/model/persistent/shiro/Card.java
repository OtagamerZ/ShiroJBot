/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.shiro;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.ImageFilters;
import jakarta.persistence.*;
import okio.Buffer;
import org.apache.commons.io.FileUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

@Entity
@Table(name = "card")
public class Card extends DAO<Card> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "name")
	private String name;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "anime_id")
	@Fetch(FetchMode.JOIN)
	private Anime anime;

	@Enumerated(EnumType.STRING)
	@Column(name = "rarity", nullable = false)
	private Rarity rarity;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Anime getAnime() {
		return anime;
	}

	public Rarity getRarity() {
		return rarity;
	}

	public BufferedImage drawCard(boolean chrome) {
		try {
			byte[] cardBytes = getImageBytes();
			assert cardBytes != null;

			try (Buffer buf = new Buffer()) {
				buf.write(cardBytes);
				BufferedImage card = ImageIO.read(buf.inputStream());

				BufferedImage frame = IO.getResourceAsImage("kawaipon/frames/new/" + rarity.name().toLowerCase() + ".png");
				assert frame != null;
				BufferedImage canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = canvas.createGraphics();

				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.drawImage(chrome ? chrome(card, false) : card, 15, 15, null);
				g2d.drawImage(chrome ? chrome(frame, true) : frame, 0, 0, null);

				g2d.dispose();

				return canvas;
			}
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return null;
		}
	}

	public BufferedImage drawUltimate(String uid) {
		try {
			byte[] cardBytes = getImageBytes();
			assert cardBytes != null;

			try (Buffer buf = new Buffer()) {
				buf.write(cardBytes);
				BufferedImage card = ImageIO.read(buf.inputStream());

				BufferedImage frame = IO.getResourceAsImage("kawaipon/frames/new/ultimate.png");
				BufferedImage nBar = IO.getResourceAsImage("kawaipon/frames/new/normal_bar.png");
				BufferedImage fBar = IO.getResourceAsImage("kawaipon/frames/new/chrome_bar.png");
				assert frame != null;
				BufferedImage canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = canvas.createGraphics();

				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				double nProg = 1; //Card.queryNative(Number.class, "SELECT cs FROM \"GetNormalCompletionState\"(:id, :anime) cs", uid, id).doubleValue();
				double fProg = 1; //Card.queryNative(Number.class, "SELECT cs FROM \"GetFoilCompletionState\"(:id, :anime) cs", uid, id).doubleValue();

				double prcnt = Math.max(nProg, fProg);
				g2d.setClip(new Rectangle(15, (int) (15 + 350 * (1 - prcnt)), 225, (int) (350 * prcnt)));
				g2d.drawImage(prcnt >= 1 ? card : ImageFilters.grayscale(card), 15, 15, null);
				g2d.setClip(null);

				g2d.drawImage(frame, 0, 0, null);

				if (nProg > 0) {
					if (nProg >= 1) {
						g2d.drawImage(nBar, 0, 0, null);
					} else {
						g2d.setClip(new Rectangle(0, (int) (82 + 295 * (1 - nProg)), frame.getWidth(), (int) (85 + 213 * nProg)));
						g2d.drawImage(nBar, 0, 0, null);
						g2d.setClip(null);
					}
				}

				if (fProg > 0) {
					if (fProg >= 1) {
						g2d.drawImage(fBar, 0, 0, null);
					} else {
						g2d.setClip(new Rectangle(0, (int) (82 + 295 * (1 - fProg)), frame.getWidth(), (int) (85 + 213 * fProg)));
						g2d.drawImage(fBar, 0, 0, null);
						g2d.setClip(null);
					}
				}

				g2d.dispose();

				return canvas;
			}
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return null;
		}
	}

	public BufferedImage drawCardNoBorder() {
		try {
			byte[] cardBytes = getImageBytes();
			assert cardBytes != null;

			try (Buffer buf = new Buffer()) {
				buf.write(cardBytes);
				return ImageIO.read(buf.inputStream());
			}
		} catch (IOException e) {
			return null;
		}
	}

	public BufferedImage drawCardNoBorder(boolean chrome) {
		try {
			byte[] cardBytes = getImageBytes();
			assert cardBytes != null;

			try (Buffer buf = new Buffer()) {
				buf.write(cardBytes);
				return chrome ? chrome(ImageIO.read(buf.inputStream()), false) : ImageIO.read(buf.inputStream());
			}
		} catch (IOException e) {
			return null;
		}
	}

	private BufferedImage chrome(BufferedImage bi, boolean border) {
		BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graph.forEachPixel(bi, (x, y, rgb) -> {
			int[] color = Graph.unpackRGB(bi.getRGB(x, y));
			int alpha = color[0];
			float[] hsv;
			if (border) {
				hsv = Color.RGBtoHSB(color[1], color[2], color[3], null);
				hsv[0] = ((hsv[0] * 360 + 180) % 360) / 360;
			} else {
				hsv = Color.RGBtoHSB(color[1], color[3], color[2], null);
				hsv[0] = ((hsv[0] * 360 + 42) % 360) / 360;
			}

			color = Graph.unpackRGB(Color.getHSBColor(hsv[0], hsv[1], hsv[2]).getRGB());

			out.setRGB(x, y, Graph.packRGB(alpha, color[1], color[2], color[3]));
		});

		return out;
	}

	private byte[] getImageBytes() throws IOException {
		File f = new File(System.getenv("CARDS_PATH") + anime.getId(), id + ".png");

		byte[] cardBytes;
		if (f.exists()) {
			File finalF = f;
			cardBytes = Main.getCacheManager().getCardCache().compute(id, (k, v) -> {
				if (v != null && v.length > 0) return v;

				try {
					return FileUtils.readFileToByteArray(finalF);
				} catch (IOException e) {
					Constants.LOGGER.error(e, e);
					return null;
				}
			});
		} else {
			try {
				cardBytes = IO.getBytes(ImageIO.read(new URL(Constants.API_ROOT + "card/" + anime.getId() + "/" + id + ".png")), "png");
			} catch (IOException e) {
				cardBytes = new byte[0];
			}

			if (cardBytes.length == 0) {
				f = IO.getResourceAsFile("kawaipon/not_found.png");
				assert f != null;

				cardBytes = FileUtils.readFileToByteArray(f);
			}
		}

		return cardBytes;
	}

	@Override
	public String toString() {
		return getName();
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
