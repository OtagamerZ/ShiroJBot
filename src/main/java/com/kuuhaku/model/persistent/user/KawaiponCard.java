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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.Quality;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.util.Calc;
import jakarta.persistence.*;
import okio.Buffer;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "kawaipon_card")
public class KawaiponCard extends DAO<KawaiponCard> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "uuid", nullable = false, unique = true, length = 36)
	private String uuid = UUID.randomUUID().toString();

	@Column(name = "chrome", nullable = false)
	private boolean chrome;

	@Column(name = "quality", nullable = false)
	private double quality = Calc.round(Math.max(0, Math.pow(ThreadLocalRandom.current().nextDouble(), 5) * 40 - 20), 1);

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	private Card card;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "kawaipon_uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	private transient StashedCard stashEntry;

	public KawaiponCard() {
	}

	public KawaiponCard(Card card, boolean chrome) {
		this.card = card;
		this.chrome = chrome;
	}

	public KawaiponCard(String uuid, Card card, boolean chrome) {
		this.uuid = uuid;
		this.card = card;
		this.chrome = chrome;
	}

	public int getId() {
		return id;
	}

	public String getUUID() {
		return uuid;
	}

	public boolean isChrome() {
		return chrome;
	}

	public void setChrome(boolean chrome) {
		this.chrome = chrome;
	}

	public double getQuality() {
		return quality;
	}

	public void setQuality(double quality) {
		this.quality = quality;
	}

	public Card getCard() {
		return card;
	}

	public String getName() {
		return (chrome ? "« %s »" : "%s").formatted(card.getName());
	}

	public int getPrice() {
		return Math.max(0, card.getRarity().getIndex() * 200);
	}

	public int getSuggestedPrice() {
		return (int) (Math.max(0, card.getRarity().getIndex() * 200) * 1.2 * Math.pow(1.0025, Math.pow(quality, 2)));
	}

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	public void setKawaipon(Kawaipon kawaipon) {
		this.kawaipon = kawaipon;
	}

	public StashedCard getStashEntry() {
		if (stashEntry == null) {
			stashEntry = DAO.query(StashedCard.class, "SELECT sc FROM StashedCard sc WHERE sc.uuid = ?1", uuid);
		}

		return stashEntry;
	}

	public BufferedImage render() {
		BufferedImage bi = card.drawCard(chrome);
		Quality q = Quality.get(quality);

		if (q.ordinal() > 0) {
			try {
				try (Buffer buf = new Buffer()) {
					buf.write(q.getOverlayBytes());
					BufferedImage overlay = ImageIO.read(buf.inputStream());

					Graphics2D g2d = bi.createGraphics();
					g2d.setRenderingHints(Constants.HD_HINTS);

					g2d.drawImage(chrome ? card.chrome(overlay, true) : overlay, 0, 0, null);
					g2d.dispose();
				}
			} catch (IOException e) {
				Constants.LOGGER.error(e, e);
				return null;
			}
		}

		return bi;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KawaiponCard that = (KawaiponCard) o;
		return chrome == that.chrome && Objects.equals(card, that.card) && getStashEntry() == null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(chrome, card);
	}
}
