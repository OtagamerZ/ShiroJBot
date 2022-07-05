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
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Charm;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONArray;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "evogear")
public class Evogear extends DAO<Evogear> implements Drawable<Evogear>, EffectHolder {
	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Column(name = "tier", nullable = false)
	private int tier;

	@Column(name = "spell", nullable = false)
	private boolean spell;

	@Convert(converter = JSONArrayConverter.class)
	@Column(name = "charms", nullable = false)
	private JSONArray charms = new JSONArray();

	@Embedded
	private CardAttributes base;

	private transient Senshi equipper = null;
	private transient CardExtra stats = new CardExtra();
	private transient Hand hand = null;
	private transient byte state = 0x2;
	/*
	0x0F
	   └ 0111
	      ││└ solid
	      │└─ available
	      └── flipped
	 */

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Card getCard() {
		return card;
	}

	public int getTier() {
		return tier + stats.getTier();
	}

	public boolean isSpell() {
		return spell;
	}

	public JSONArray getCharms() {
		return charms;
	}

	public CardAttributes getBase() {
		return base;
	}

	public Senshi getEquipper() {
		return equipper;
	}

	public void setEquipper(Senshi equipper) {
		this.equipper = equipper;
	}

	public CardExtra getStats() {
		return stats;
	}

	public java.util.List<String> getTags() {
		List<String> out = new ArrayList<>();
		if (hasEffect()) {
			out.add("tag/effect");
		}
		for (Object tag : base.getTags()) {
			out.add("tag/" + tag);
		}

		return out;
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
	public String getDescription(I18N locale) {
		return Utils.getOr(stats.getDescription(locale), base.getDescription(locale));
	}

	@Override
	public int getMPCost() {
		return (int) ((base.getMana() + stats.getMana()) * getCostMult());
	}

	@Override
	public int getHPCost() {
		return (int) ((base.getBlood() + stats.getBlood()) * getCostMult());
	}

	@Override
	public int getDmg() {
		return (int) ((base.getAtk() + stats.getAtk()) * getAttrMult());
	}

	@Override
	public int getDef() {
		return (int) ((base.getDef() + stats.getDef()) * getAttrMult());
	}

	@Override
	public int getDodge() {
		return (int) ((base.getDodge() + stats.getDodge()) * getAttrMult());
	}

	@Override
	public int getBlock() {
		return (int) ((base.getBlock() + stats.getBlock()) * getAttrMult());
	}

	private double getCostMult() {
		double mult = 1;
		if (hand != null && spell && hand.getOrigin().minor() == Race.MYSTICAL) {
			mult *= 0.9 - (hand.getUserDeck().countRace(Race.MYSTICAL) * 0.01);
		}

		return mult;
	}

	private double getAttrMult() {
		double mult = 1;
		if (hand != null && !spell && hand.getOrigin().minor() == Race.MACHINE) {
			mult *= 1.1 + (hand.getUserDeck().countRace(Race.MACHINE) * 0.01);
		}

		return mult;
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
	public boolean isFlipped() {
		if (equipper != null) {
			return equipper.isFlipped() || Bit.on(state, 2);
		}

		return Bit.on(state, 2);
	}

	@Override
	public void setFlipped(boolean flipped) {
		state = (byte) Bit.set(state, 2, flipped);
	}

	public String getEffect() {
		return Utils.getOr(stats.getEffect(), base.getEffect());
	}

	public boolean hasEffect() {
		return !getEffect().isEmpty();
	}

	@Override
	public boolean execute(EffectParameters ep) {
		String effect = Utils.getOr(stats.getEffect(), base.getEffect());
		if (effect.isBlank() || !effect.contains(ep.trigger().name())) return false;

		try {
			GroovyShell gs = new GroovyShell();
			gs.setVariable("ep", ep);
			gs.setVariable("self", this);
			gs.setVariable("pow", 1 + stats.getPower());
			gs.evaluate(effect);

			return true;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute " + card.getName() + " effect", e);
			return false;
		}
	}

	@Override
	public void reset() {
		stats = new CardExtra();
		state = 0x2;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		if (isFlipped()) return deck.getFrame().getBack(deck);

		String desc = getDescription(locale);

		BufferedImage img = card.drawCardNoBorder(deck.isUsingFoil());
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		g2d.setClip(deck.getFrame().getBoundary());
		g2d.drawImage(img, 0, 0, null);
		g2d.setClip(null);

		g2d.drawImage(deck.getFrame().getFront(!desc.isEmpty()), 0, 0, null);

		g2d.setFont(new Font("Arial", Font.BOLD, 20));
		g2d.setColor(deck.getFrame().getPrimaryColor());
		Graph.drawOutlinedString(g2d, StringUtils.abbreviate(card.getName(), MAX_NAME_LENGTH), 10, 30, 2, deck.getFrame().getBackgroundColor());

		if (!getCharms().isEmpty()) {
			List<BufferedImage> icons = charms.stream()
					.map(String::valueOf)
					.map(Charm::valueOf)
					.map(Charm::getIcon)
					.filter(Objects::nonNull)
					.limit(2)
					.toList();

			if (!icons.isEmpty()) {
				Graph.applyTransformed(g2d, 200 - 64, 55, g -> {
					if (icons.size() == 1) {
						g.drawImage(icons.get(0), 0, 0, null);
					} else {
						BufferedImage mask = IO.getResourceAsImage("shoukan/charm/mask.png");
						assert mask != null;

						for (int i = 0; i < icons.size(); i++) {
							BufferedImage icon = icons.get(i);
							Graph.applyMask(icon, mask, i, true);
							g.drawImage(icon, 0, 0, null);
						}
						g.drawImage(IO.getResourceAsImage("shoukan/charm/div.png"), 0, 0, null);
					}
				});
			}
		}

		if (!desc.isEmpty()) {
			g2d.setColor(deck.getFrame().getSecondaryColor());
			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 12));
			g2d.drawString(getTags().stream().map(locale::get).map(String::toUpperCase).toList().toString(), 7, 275);

			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 10));
			Graph.drawMultilineString(g2d,
					StringUtils.abbreviate(desc, MAX_DESC_LENGTH), 7, 287, 211, 3,
					parseValues(stats).andThen(s -> {
						if (s.startsWith("\u200B")) {
							g2d.setColor(Graph.invert(deck.getFrame().getThemeColor()));
						} else {
							g2d.setColor(deck.getFrame().getSecondaryColor());
						}

						return s;
					})
			);
		}

		drawCosts(g2d);
		drawAttributes(g2d, !desc.isEmpty());

		if (!isAvailable()) {
			RescaleOp op = new RescaleOp(0.5f, 0, null);
			op.filter(out, out);
		}

		g2d.dispose();

		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Evogear evogear = (Evogear) o;
		return Objects.equals(id, evogear.id) && Objects.equals(card, evogear.card);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card);
	}

	@Override
	public Evogear clone() throws CloneNotSupportedException {
		return (Evogear) super.clone();
	}

	@Override
	public String toString() {
		return card.getName();
	}
}
