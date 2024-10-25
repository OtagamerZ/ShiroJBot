/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import okio.Buffer;
import org.apache.commons.io.FileUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "card", schema = "kawaipon", indexes = @Index(columnList = "anime_id, id"))
public class Card extends DAO<Card> implements Serializable {
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

	public Card() {
	}

	public Card(Actor act, I18N locale) {
		this.id = (act instanceof Hero ? "H" : "M") + ":" + act.getId();
		this.name = act.getName(locale);
		this.anime = DAO.find(Anime.class, "NO_SYNC");
		this.rarity = act instanceof Hero ? Rarity.HERO : Rarity.MONSTER;
	}

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
			try (Buffer buf = new Buffer(); InputStream is = buf.inputStream()) {
				buf.write(getImageBytes());
				BufferedImage card = ImageIO.read(is);

				buf.clear();
				buf.write(rarity.getFrameBytes());
				BufferedImage frame = ImageIO.read(is);

				BufferedImage canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = canvas.createGraphics();
				g2d.setRenderingHints(Constants.HD_HINTS);

				if (chrome) {
					chrome(card, false);
					chrome(frame, true);
				}

				g2d.drawImage(card, 15, 15, null);
				g2d.drawImage(frame, 0, 0, null);

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

			try (Buffer buf = new Buffer(); InputStream is = buf.inputStream()) {
				buf.write(cardBytes);
				return ImageIO.read(is);
			}
		} catch (IOException e) {
			return null;
		}
	}

	public BufferedImage drawCardNoBorder(boolean chrome) {
		try {
			byte[] cardBytes = getImageBytes();
			assert cardBytes != null;

			try (Buffer buf = new Buffer(); InputStream is = buf.inputStream()) {
				buf.write(cardBytes);
				BufferedImage img = ImageIO.read(is);

				if (chrome) {
					chrome(img, false);
				}

				return img;
			}
		} catch (IOException e) {
			return null;
		}
	}

	public void chrome(BufferedImage bi, boolean border) {
		Graph.forEachPixel(bi, (x, y, rgb) -> {
			int[] color = Graph.unpackRGB(rgb);
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
			return Graph.packRGB(alpha, color[1], color[2], color[3]);
		});
	}

	private byte[] getImageBytes() {
		String path;
		switch (rarity) {
			case HERO, MONSTER -> path = Constants.CARDS_ROOT + (rarity == Rarity.HERO ? "../heroes" : "../monsters");
			default -> path = Constants.CARDS_ROOT + anime.getId();
		}

		byte[] cardBytes = Main.getCacheManager().computeResource(id, (k, v) -> {
			if (v != null && v.length > 0) return v;

			try {
				String key = Utils.equalsAny(rarity, Rarity.HERO, Rarity.MONSTER) ? id.split(":")[1] : id;

				File f = new File(path, key + ".png");
				if (f.exists()) {
					return FileUtils.readFileToByteArray(f);
				} else if (!Utils.equalsAny(rarity, Rarity.HERO, Rarity.MONSTER)) {
					return IO.getImageBytes(Constants.API_ROOT + "card/" + anime.getId() + "/" + key + ".png");
				}
			} catch (IOException e) {
				Constants.LOGGER.error(e, e);
			}

			return new byte[0];
		});

		if (cardBytes.length == 0) {
			cardBytes = IO.getBytes(IO.getResourceAsImage("kawaipon/not_found.png"), "png");
		}

		return cardBytes;
	}

	public Senshi asSenshi() {
		return DAO.find(Senshi.class, id);
	}

	public Evogear asEvogear() {
		return DAO.find(Evogear.class, id);
	}

	public Field asField() {
		return DAO.find(Field.class, id);
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
		return Objects.hashCode(id);
	}
}
