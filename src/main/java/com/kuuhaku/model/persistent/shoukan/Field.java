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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.FieldType;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "field")
public class Field extends DAO<Field> implements Drawable<Field> {
	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Convert(converter = JSONObjectConverter.class)
	@Column(name = "modifiers", nullable = false)
	private JSONObject modifiers = new JSONObject();

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private FieldType type = FieldType.NONE;

	@Column(name = "effect", nullable = false)
	private boolean effect = false;

	private transient Hand hand = null;
	private transient byte state = 0x2;
	/*
	0x0F
	   └ 0011
	       │└ solid
	       └─ available
	 */

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Card getCard() {
		return card;
	}

	public JSONObject getModifiers() {
		return modifiers;
	}

	public void setModifiers(JSONObject modifiers) {
		this.modifiers = modifiers;
	}

	public FieldType getType() {
		return type;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	public boolean isEffect() {
		return effect;
	}

	@Override
	public Hand getHand() {
		return hand;
	}

	@Override
	public void setHand(Hand hand) {
		this.hand = hand;
	}

	@Override
	public boolean isSolid() {
		return Bit.on(state, 0);
	}

	@Override
	public void setSolid(boolean solid) {
		state = (byte) Bit.set(state, 0, solid);
	}

	@Override
	public boolean isAvailable() {
		return Bit.on(state, 1);
	}

	@Override
	public void setAvailable(boolean available) {
		state = (byte) Bit.set(state, 1, available);
	}

	@Override
	public void reset() {
		if (isSolid()) {
			state = 0x3;
		} else {
			state = 0x2;
		}
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		BufferedImage img = getVanity().drawCardNoBorder(false);
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		g2d.setClip(deck.getFrame().getBoundary());
		g2d.drawImage(img, 0, 0, null);
		g2d.setClip(null);

		g2d.drawImage(deck.getFrame().getFront(false), 0, 0, null);

		g2d.setFont(new Font("Arial", Font.BOLD, 18));
		g2d.setColor(deck.getFrame().getPrimaryColor());
		String name = StringUtils.abbreviate(card.getName(), MAX_NAME_LENGTH);
		Graph.drawOutlinedString(g2d, name, 12, 30, 2, deck.getFrame().getBackgroundColor());

		if (type != FieldType.NONE) {
			BufferedImage icon = type.getIcon();
			assert icon != null;

			g2d.drawImage(icon, 200 - icon.getWidth(), 55, null);
		}

		g2d.setFont(new Font("Arial", Font.BOLD, 18));
		FontMetrics m = g2d.getFontMetrics();

		int i = 0;
		for (Map.Entry<String, Object> entry : modifiers.entrySet()) {
			Race r = Race.valueOf(entry.getKey());
			double mod = (double) entry.getValue();
			if (mod == 0) continue;

			int y = 279 - 25 * i++;

			BufferedImage icon = r.getIcon();
			g2d.drawImage(icon, 23, y, null);
			g2d.setColor(r.getColor());
			Graph.drawOutlinedString(g2d, Utils.sign((int) ((1 + mod) * 100)) + "%",
					23 + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2,
					2, Color.BLACK
			);
		}

		if (!isAvailable()) {
			RescaleOp op = new RescaleOp(0.5f, 0, null);
			op.filter(out, out);
		}

		g2d.dispose();

		return out;
	}

	public BufferedImage renderBackground() {
		BufferedImage bi = IO.getResourceAsImage("shoukan/backdrop.webp");
		assert bi != null;

		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		BufferedImage cover = IO.getResourceAsImage("shoukan/arenas/" + id + ".webp");
		g2d.drawImage(cover, 0, 0, null);
		g2d.dispose();

		return bi;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		return Objects.equals(id, field.id) && Objects.equals(card, field.card) && Objects.equals(modifiers, field.modifiers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card, modifiers);
	}

	@Override
	public Field clone() throws CloneNotSupportedException {
		return (Field) super.clone();
	}

	@Override
	public String toString() {
		return card.getName();
	}
}
