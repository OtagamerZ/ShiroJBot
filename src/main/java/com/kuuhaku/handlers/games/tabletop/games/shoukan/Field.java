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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.ArenaField;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

@Entity
public class Field implements Drawable, Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@OneToOne(fetch = FetchType.EAGER)
	private Card card;

	@Enumerated(EnumType.STRING)
	private ArenaField field;

	private transient Account acc = null;
	private transient boolean available = true;

	@Override
	public BufferedImage drawCard(Account acc, boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);

			g2d.drawImage(acc.getFrame().getFrontEquipment(), 0, 0, null);
			g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

			Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

			Color[] colors = {
					Color.decode("#8d01ff"),
					Color.green,
					Color.decode("#2c01ff"),
					Color.orange,
					Color.yellow,
					Color.cyan,
					Color.decode("#993500"),
					Color.white,
					Color.red
			};
			int i = 0;
			for (Race r : new Race[]{Race.HUMAN, Race.ELF, Race.BESTIAL, Race.MACHINE, Race.DIVINITY, Race.MYSTICAL, Race.CREATURE, Race.SPIRIT, Race.DEMON}) {
				g2d.setColor(colors[i]);
				Profile.drawOutlinedText(Helper.toPercent(field.getModifiers().getOrDefault(r, 1f)), 86 + (28 * i), 45, g2d);
				i++;
			}
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

		g2d.drawImage(FrameColor.PINK.getFrontEquipment(), 0, 0, null);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

		Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

		Color[] colors = {
				Color.decode("#8d01ff"),
				Color.green,
				Color.decode("#2c01ff"),
				Color.orange,
				Color.yellow,
				Color.cyan,
				Color.decode("#993500"),
				Color.white,
				Color.red
		};
		int i = 0;
		for (Race r : new Race[]{Race.HUMAN, Race.ELF, Race.BESTIAL, Race.MACHINE, Race.DIVINITY, Race.MYSTICAL, Race.CREATURE, Race.SPIRIT, Race.DEMON}) {
			g2d.setColor(colors[i]);
			Profile.drawOutlinedText(Helper.toPercent(field.getModifiers().getOrDefault(r, 1f)), 86 + (28 * i), 45, g2d);
			i++;
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

	public ArenaField getField() {
		return field;
	}

	@Override
	public boolean isFlipped() {
		return false;
	}

	@Override
	public void setFlipped(boolean flipped) {
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
	}

	@Override
	public Account getAcc() {
		return acc;
	}

	@Override
	public void setAcc(Account acc) {
		this.acc = acc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		return id == field.id;
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
