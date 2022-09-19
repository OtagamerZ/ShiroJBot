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
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.util.*;
import com.kuuhaku.util.json.JSONArray;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "field")
public class Field extends DAO<Field> implements Drawable<Field> {
	@Transient
	public final String KLASS = getClass().getName();

	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Column(name = "modifiers", nullable = false)
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject modifiers = new JSONObject();

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private FieldType type = FieldType.NONE;

	@Column(name = "effect", nullable = false)
	private boolean effect = false;

	@Column(name = "tags", nullable = false)
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray tags = new JSONArray();

	private transient Hand hand = null;

	@Transient
	private byte state = 0b10;
	/*
	0xF
	  └ 000 0011
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

	public JSONArray getRawTags() {
		return tags;
	}

	@Override
	public List<String> getTags() {
		return tags.stream().map(t -> "tag/" + t).toList();
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
	public boolean keepOnDestroy() {
		return !isEffect();
	}

	@Override
	public void reset() {
		state = 0b11;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		DeckStyling style = deck.getStyling();
		BufferedImage img = getVanity().drawCardNoBorder(false);
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		g2d.setClip(style.getFrame().getBoundary());
		g2d.drawImage(img, 0, 0, null);
		g2d.setClip(null);

		g2d.drawImage(style.getFrame().getFront(false), 0, 0, null);

		g2d.setFont(FONT);
		g2d.setColor(style.getFrame().getPrimaryColor());
		String name = Graph.abbreviate(g2d, getVanity().getName(), MAX_NAME_WIDTH);
		Graph.drawOutlinedString(g2d, name, 12, 30, 2, style.getFrame().getBackgroundColor());

		if (type != FieldType.NONE) {
			BufferedImage icon = type.getIcon();
			assert icon != null;

			g2d.drawImage(icon, 200 - icon.getWidth(), 55, null);
		}

		g2d.setFont(FONT);
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
			Graph.drawOutlinedString(g2d, Utils.sign((int) (mod * 100)) + "%",
					23 + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2,
					BORDER_WIDTH, Color.BLACK
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
		BufferedImage bi =  IO.getResourceAsImage("shoukan/arenas/" + id + ".webp");
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		BufferedImage aux = IO.getResourceAsImage("shoukan/side/middle.webp");
		g2d.drawImage(aux, bi.getWidth() / 2 - aux.getWidth() / 2, bi.getHeight() / 2 - aux.getHeight() / 2, null);

		aux = IO.getResourceAsImage("shoukan/overlay/middle.webp");
		g2d.drawImage(aux, bi.getWidth() / 2 - aux.getWidth() / 2, bi.getHeight() / 2 - aux.getHeight() / 2, null);

		g2d.dispose();

		return bi;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		return Objects.equals(id, field.id)
				&& Objects.equals(card, field.card)
				&& Objects.equals(modifiers, field.modifiers);
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

	public static Field getRandom() {
		String id = DAO.queryNative(String.class, "SELECT card_id FROM field ORDER BY RANDOM()");
		if (id == null) return null;

		return DAO.find(Field.class, id);
	}

	public static Field getRandom(String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM field");
		for (String f : filters) {
			query.appendNewLine(f);
		}
		query.appendNewLine("ORDER BY RANDOM()");

		String id = DAO.queryNative(String.class, query.toString());
		if (id == null) return null;

		return DAO.find(Field.class, id);
	}
}
