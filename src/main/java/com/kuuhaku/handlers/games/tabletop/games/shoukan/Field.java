/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "field")
public class Field implements Drawable, Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@OneToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT '{}'")
	private String modifiers = "{}";

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean day = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean effectOnly = false;

	private transient Shoukan game = null;
	private transient Account acc = null;
	private transient boolean available = true;

	public Field() {
	}

	@Override
	public BufferedImage drawCard(boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			g2d.setClip(new Polygon(
					new int[]{13, 212, 223, 223, 212, 13, 2, 2},
					new int[]{2, 2, 13, 337, 348, 348, 337, 13},
					8
			));
			g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);
			g2d.setClip(null);

			g2d.drawImage(acc.getFrame().getFront(false), 0, 0, null);
			g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));

			Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

			Map<Race, String> colors = Map.of(
					Race.HUMAN, "#9013fe",
					Race.ELF, "#7ed321",
					Race.BESTIAL, "#ffe0af",
					Race.MACHINE, "#f5a623",
					Race.DIVINITY, "#f8e71c",
					Race.MYSTICAL, "#4fe4c3",
					Race.CREATURE, "#8b572a",
					Race.SPIRIT, "#ffffff",
					Race.DEMON, "#d0021b",
					Race.UNDEAD, "#fd88fd"
			);
			int y = 80;
			int i = 0;
			for (Map.Entry<String, Object> entry : getModifiers().entrySet()) {
				Race r = Race.valueOf(entry.getKey());
				float modif = ((float) entry.getValue()) - 1;
				BufferedImage icon = r.getIcon();
				if (icon == null) continue;

				g2d.setColor(Color.decode(colors.get(r)));
				g2d.drawImage(icon, 29, y + (25 * i), 23, 23, null);
				Profile.drawOutlinedText((modif > 0 ? "+" : "") + Helper.roundToString(modif * 100, 0) + "%", 57, y + 21 + (25 * i), g2d);
				i++;
			}
		}

		if (day)
			g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/day.png"), 135, 58, null);
		else
			g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/night.png"), 135, 58, null);

		if (!available) {
			g2d.setColor(new Color(0, 0, 0, 150));
			g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		}

		g2d.dispose();

		return bi;
	}

	public int getId() {
		return id;
	}

	@Override
	public Card getCard() {
		return card;
	}

	public String getField() {
		return card.getId();
	}

	public JSONObject getModifiers() {
		return new JSONObject(modifiers);
	}

	public void setModifiers(String modifiers) {
		this.modifiers = modifiers;
	}

	public boolean isDay() {
		return day;
	}

	public void setDay(boolean day) {
		this.day = day;
	}

	public boolean isEffectOnly() {
		return effectOnly;
	}

	public void setEffectOnly(boolean effectOnly) {
		this.effectOnly = effectOnly;
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
	public Shoukan getGame() {
		return game;
	}

	@Override
	public void setGame(Shoukan game) {
		this.game = game;
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
	public void bind(Hand h) {
		this.game = h.getGame();
		this.acc = h.getAcc();
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
	public Field copy() {
		try {
			return (Field) clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public String toString() {
		return new JSONObject(card.toString()) {{
			put("field", new JSONObject() {{
				put("id", id);
				put("modifiers", getModifiers());
			}});
		}}.toString();
	}

	public String getBase64() {
		return Helper.atob(drawCard(false), "png");
	}
}
