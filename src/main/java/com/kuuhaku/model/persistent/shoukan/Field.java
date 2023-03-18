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
import com.kuuhaku.model.common.XList;
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
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import kotlin.Pair;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "field")
public class Field extends DAO<Field> implements Drawable<Field> {
	@Transient
	public final String KLASS = getClass().getName();
	public transient long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Type(JsonBinaryType.class)
	@Column(name = "modifiers", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject modifiers = new JSONObject();

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private FieldType type = FieldType.NONE;

	@Column(name = "effect", nullable = false)
	private boolean effect = false;

	@Type(JsonBinaryType.class)
	@Column(name = "tags", nullable = false, columnDefinition = "JSONB")
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
	public long getSerial() {
		return SERIAL;
	}

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
		return tags.stream().map(t -> "tag/" + ((String) t).toLowerCase()).toList();
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

	public boolean isActive() {
		return hand.getGame().getArena().getField().equals(this);
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
		BufferedImage out = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		DeckStyling style = deck.getStyling();
		Graph.applyTransformed(g2d, 15, 15, g1 -> {
			if (isFlipped()) {
				g1.drawImage(style.getFrame().getBack(deck), 15, 15, null);
			} else {
				BufferedImage img = getVanity().drawCardNoBorder(false);

				g1.setClip(style.getFrame().getBoundary());
				g1.drawImage(img, 0, 0, null);
				g1.setClip(null);

				g1.drawImage(style.getFrame().getFront(false), 0, 0, null);

				g1.setFont(FONT);
				g1.setColor(style.getFrame().getPrimaryColor());
				String name = Graph.abbreviate(g1, getVanity().getName(), MAX_NAME_WIDTH);
				Graph.drawOutlinedString(g1, name, 12, 30, 2, style.getFrame().getBackgroundColor());

				if (type != FieldType.NONE) {
					BufferedImage icon = type.getIcon();
					assert icon != null;

					g1.drawImage(icon, 200 - icon.getWidth(), 55, null);
				}

				g1.setFont(FONT);
				FontMetrics m = g1.getFontMetrics();

				List<Pair<Race, Double>> mods = modifiers.entrySet().stream()
						.map(e -> new Pair<>(Race.valueOf(e.getKey()), ((Number) e.getValue()).doubleValue()))
						.sorted(Comparator.comparing(p -> -p.getSecond()))
						.toList();

				int i = 0;
				for (Pair<Race, Double> mod : mods) {
					if (mod.getSecond() == 0) continue;

					int y = 279 - 25 * i++;

					BufferedImage icon = mod.getFirst().getIcon();
					g1.drawImage(icon, 23, y, null);
					g1.setColor(mod.getFirst().getColor());
					Graph.drawOutlinedString(g1, Utils.sign((int) (mod.getSecond() * 100)) + "%",
							23 + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2,
							BORDER_WIDTH, Color.BLACK
					);
				}

				if (!isAvailable()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}
			}
		});

		g2d.dispose();

		return out;
	}

	public BufferedImage renderBackground() {
		BufferedImage bi =  IO.getResourceAsImage("shoukan/arenas/" + id + ".jpg");
		if (bi == null) {
			bi = IO.getResourceAsImage("shoukan/arenas/DEFAULT.jpg");
		}

		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		BufferedImage aux = IO.getResourceAsImage("shoukan/side/middle.png");
		g2d.drawImage(aux, bi.getWidth() / 2 - aux.getWidth() / 2, bi.getHeight() / 2 - aux.getHeight() / 2, null);

		aux = IO.getResourceAsImage("shoukan/overlay/middle.png");
		g2d.drawImage(aux, bi.getWidth() / 2 - aux.getWidth() / 2, bi.getHeight() / 2 - aux.getHeight() / 2, null);

		g2d.dispose();

		return bi;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		return Objects.equals(id, field.id) && Objects.equals(card, field.card);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card);
	}

	@Override
	public Field clone() throws CloneNotSupportedException {
		Field clone = (Field) super.clone();
		clone.modifiers = new JSONObject(modifiers);
		clone.tags = new JSONArray(tags);

		return clone;
	}

	@Override
	public String toString() {
		return card.getName();
	}

	public static Field getRandom() {
		String id = DAO.queryNative(String.class, "SELECT card_id FROM field WHERE NOT effect ORDER BY RANDOM()");
		if (id == null) return null;

		return DAO.find(Field.class, id);
	}

	public static Field getRandom(String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM field");
		for (String f : filters) {
			query.appendNewLine(f);
		}

		if (filters.length == 0) {
			query.appendNewLine("WHERE NOT effect");
		} else {
			query.appendNewLine("AND NOT effect");
		}

		query.appendNewLine("ORDER BY RANDOM()");

		String id = DAO.queryNative(String.class, query.toString());
		if (id == null) return null;

		return DAO.find(Field.class, id);
	}

	public static XList<Field> getByTag(String... tags) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT by_tag('field', ?1)", (Object[]) tags);

		return new XList<>(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE f.card.id IN ?1", ids));
	}
}
