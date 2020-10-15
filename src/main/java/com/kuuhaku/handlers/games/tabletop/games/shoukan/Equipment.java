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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

@Entity
@Table(name = "equipment")
public class Equipment implements Drawable, Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@OneToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int atk;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int def;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int tier;

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient Pair<Integer, Card> linkedTo = null;

	@Override
	public BufferedImage drawCard(Account acc, boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);

			if (linkedTo != null) {
				g2d.drawImage(linkedTo.getRight().drawCardNoBorder(), 20, 52, 60, 93, null);
				g2d.setClip(null);
			}

			g2d.drawImage(acc.getFrame().getFrontEquipment(), 0, 0, null);
			g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

			Profile.drawOutlinedText(card.getName(), 13, 30, g2d);

			g2d.setColor(Color.red);
			Profile.drawOutlinedText(String.valueOf(atk), 45, 316, g2d);

			g2d.setColor(Color.green);
			Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 316, g2d);
		}

		if (!available) {
			g2d.setColor(new Color(0, 0, 0, 150));
			g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		}

		g2d.dispose();

		return bi;
	}

	public BufferedImage drawCard() {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);

		if (linkedTo != null) {
			g2d.drawImage(linkedTo.getRight().drawCardNoBorder(), 20, 52, 60, 93, null);
			g2d.setClip(null);
		}

		g2d.drawImage(FrameColor.PINK.getFrontEquipment(), 0, 0, null);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

		Profile.drawOutlinedText(card.getName(), 13, 32, g2d);

		g2d.setColor(Color.red);
		Profile.drawOutlinedText(String.valueOf(atk), 45, 316, g2d);

		g2d.setColor(Color.green);
		Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 316, g2d);

		try {
			BufferedImage star = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/star.png")));
			for (int i = 0; i < tier; i++)
				g2d.drawImage(star, (bi.getWidth() / 2) - ((star.getWidth() * tier / 2) + star.getWidth() * i), 42, null);
		} catch (IOException ignore) {
		}

		if (!available) {
			g2d.setColor(new Color(0, 0, 0, 150));
			g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		}

		g2d.dispose();

		return bi;
	}

	@Override
	public Card getCard() {
		return card;
	}

	@Override
	public boolean isFlipped() {
		return flipped;
	}

	@Override
	public void setFlipped(boolean flipped) {
		this.flipped = flipped;
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
	}

	public Pair<Integer, Card> getLinkedTo() {
		return linkedTo;
	}

	public void setLinkedTo(Pair<Integer, Card> linkedTo) {
		this.linkedTo = linkedTo;
	}

	public int getAtk() {
		return atk;
	}

	public int getDef() {
		return def;
	}

	public int getTier() {
		return tier;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Equipment champion = (Equipment) o;
		return id == champion.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public Drawable copy() {
		try {
			return (Drawable) clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
