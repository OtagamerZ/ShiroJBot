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
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.FieldType;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.utils.Bit;
import com.kuuhaku.utils.Graph;
import com.kuuhaku.utils.IO;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.json.JSONObject;
import kotlin.Pair;
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
	public transient final long SERIAL = Constants.DEFAULT_RNG.nextLong();

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

	private transient Pair<Integer, BufferedImage> cache = null;
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
		cache = null;
		state = 0x2;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		int hash = renderHashCode(locale);
		if (cache == null || cache.getFirst() != hash) {
			BufferedImage img = getVanity().drawCardNoBorder(false);
			BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = out.createGraphics();
			g2d.setRenderingHints(Constants.HD_HINTS);

			g2d.setClip(deck.getFrame().getBoundary());
			g2d.drawImage(img, 0, 0, null);
			g2d.setClip(null);

			g2d.drawImage(deck.getFrame().getFront(false), 0, 0, null);

			g2d.setFont(Fonts.STAATLICHES.deriveFont(Font.BOLD, 22));
			g2d.setColor(deck.getFrame().getPrimaryColor());
			Graph.drawOutlinedString(g2d, StringUtils.abbreviate(card.getName(), MAX_NAME_LENGTH), 10, 30, 2, deck.getFrame().getBackgroundColor());

			if (type != FieldType.NONE) {
				BufferedImage icon = type.getIcon();
				assert icon != null;

				g2d.drawImage(icon, 200 - icon.getWidth(), 55, null);
			}

			g2d.setFont(Fonts.STAATLICHES.deriveFont(Font.BOLD, 20));
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

			cache = new Pair<>(hash, out);
		}

		return cache.getSecond();
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
	public int renderHashCode(I18N locale) {
		return Objects.hash(hand, locale);
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
