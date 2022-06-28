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
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.CardState;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.utils.Bit;
import com.kuuhaku.utils.Graph;
import com.kuuhaku.utils.IO;
import com.kuuhaku.utils.Utils;
import groovy.lang.GroovyShell;
import kotlin.Pair;
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
@Table(name = "senshi")
public class Senshi extends DAO<Senshi> implements Drawable<Senshi>, EffectHolder {
	public transient final long SERIAL = Constants.DEFAULT_RNG.nextLong();

	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Enumerated(EnumType.STRING)
	@Column(name = "race", nullable = false)
	private Race race;

	@Embedded
	private CardAttributes base;

	private transient Pair<Integer, BufferedImage> cache = null;
	private transient List<Evogear> equipments = new BondedList<>(e -> {
		e.setEquipper(this);
		e.setHand(getHand());
	});
	private transient CardExtra stats = new CardExtra();
	private transient SlotColumn slot = null;
	private transient Hand hand = null;
	private transient int state = 0x2;
	/*
	0x00 00 FFF F
	        │││ └ 1111
	        │││   │││└ solid
	        │││   ││└─ available
	        │││   │└── defending
	        │││   └─── flipped
	        ││└─ (0 - 15) stunned
	        │└── (0 - 15) sleeping
	        └─── (0 - 15) stasis
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
	public Card getVanity() {
		return Utils.getOr(stats.getVanity(), card);
	}

	public Race getRace() {
		return Utils.getOr(stats.getRace(), race);
	}

	public CardAttributes getBase() {
		return base;
	}

	public CardExtra getStats() {
		return stats;
	}

	public List<String> getTags() {
		List<String> out = new ArrayList<>();
		out.add("race/" + race.name());
		if (hasEffect()) {
			out.add("tag/effect");
		}
		for (Object tag : base.getTags()) {
			out.add("tag/" + tag);
		}

		return out;
	}

	public List<Evogear> getEquipments() {
		if (equipments.size() > 3) {
			equipments = equipments.subList(0, 3);
		}

		return equipments;
	}

	@Override
	public SlotColumn getSlot() {
		return slot;
	}

	@Override
	public void setSlot(SlotColumn slot) {
		this.slot = slot;
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
		return base.getMana() + stats.getMana();
	}

	@Override
	public int getHPCost() {
		return base.getBlood() + stats.getBlood();
	}

	@Override
	public int getDmg() {
		return base.getAtk() + stats.getAtk() + equipments.stream().mapToInt(Evogear::getDmg).sum();
	}

	@Override
	public int getDef() {
		return base.getDef() + stats.getDef() + equipments.stream().mapToInt(Evogear::getDef).sum();
	}

	public int getActiveAttr() {
		if (isDefending()) return getDef();
		return getDmg();
	}

	@Override
	public int getDodge() {
		return base.getDodge() + stats.getDodge() + equipments.stream().mapToInt(Evogear::getDodge).sum();
	}

	@Override
	public int getBlock() {
		return base.getBlock() + stats.getBlock() + equipments.stream().mapToInt(Evogear::getBlock).sum();
	}

	@Override
	public boolean isSolid() {
		return Bit.on(state, 0);
	}

	@Override
	public void setSolid(boolean solid) {
		state = Bit.set(state, 0, solid);
	}

	@Override
	public boolean isAvailable() {
		return Bit.on(state, 1);
	}

	@Override
	public void setAvailable(boolean available) {
		state = Bit.set(state, 1, available);
	}

	public boolean isDefending() {
		return isFlipped() || Bit.on(state, 2);
	}

	public void setDefending(boolean defending) {
		state = Bit.set(state, 2, defending);
	}

	@Override
	public boolean isFlipped() {
		return Bit.on(state, 3);
	}

	@Override
	public void setFlipped(boolean flipped) {
		if (isFlipped() && !flipped) {
			setDefending(true);
		}

		state = Bit.set(state, 3, flipped);
	}

	public boolean isStunned() {
		return Bit.on(state, 1, 4);
	}

	public void setStun(int time) {
		int curr = Bit.get(state, 1, 4);
		state = Bit.set(state, 1, Math.max(curr, time), 4);
	}

	public void reduceStun(int time) {
		int curr = Bit.get(state, 1, 4);
		state = Bit.set(state, 1, Math.max(0, curr - time), 4);
	}

	public boolean isSleeping() {
		return Bit.on(state, 2, 4);
	}

	public void setSleep(int time) {
		int curr = Bit.get(state, 2, 4);
		state = Bit.set(state, 2, Math.max(curr, time), 4);
	}

	public void reduceSleep(int time) {
		int curr = Bit.get(state, 2, 4);
		state = Bit.set(state, 2, Math.max(0, curr - time), 4);
	}

	public boolean isStasis() {
		return Bit.on(state, 3, 4);
	}

	public void setStasis(int time) {
		int curr = Bit.get(state, 3, 4);
		state = Bit.set(state, 3, Math.max(curr, time), 4);
	}

	public void reduceStasis(int time) {
		int curr = Bit.get(state, 3, 4);
		state = Bit.set(state, 3, Math.max(0, curr - time), 4);
	}

	public CardState getState() {
		if (isFlipped()) return CardState.FLIPPED;
		else if (isDefending()) return CardState.DEFENSE;

		return CardState.ATTACK;
	}

	public boolean isSupporting() {
		return slot != null && slot.hasBottom() && slot.getBottom().SERIAL == SERIAL;
	}

	public String getEffect() {
		return Utils.getOr(stats.getEffect(), base.getEffect());
	}

	public boolean hasEffect() {
		return !getEffect().isEmpty();
	}

	@Override
	public boolean execute(EffectParameters ep) {
		String effect = getEffect();
		if (effect.isBlank() || !effect.contains(ep.trigger().name()) || base.isLocked()) return false;

		//Hand other = ep.getHands().get(ep.getOtherSide());
		try {
			base.lock();

			/*if (hero != null) {
				other.setHeroDefense(true);
			}*/

			GroovyShell gs = new GroovyShell();
			gs.setVariable("ep", ep);
			gs.setVariable("self", this);
			gs.setVariable("pow", 1 + stats.getPower());
			gs.evaluate(effect);

			return true;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute " + card.getName() + " effect", e);
			return false;
		} finally {
			//other.setHeroDefense(false);
		}
	}

	@Override
	public void reset() {
		cache = null;
		equipments = new ArrayList<>();
		stats = new CardExtra();
		slot = null;
		state = 0x2;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		int hash = renderHashCode(locale);
		if (cache == null || cache.getFirst() != hash) {
			if (isFlipped()) return deck.getFrame().getBack(deck);

			String desc = getDescription(locale);

			BufferedImage img = getVanity().drawCardNoBorder(deck.isUsingFoil());
			BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = out.createGraphics();
			g2d.setRenderingHints(Constants.HD_HINTS);

			g2d.setClip(deck.getFrame().getBoundary());
			g2d.drawImage(img, 0, 0, null);
			g2d.setClip(null);

			g2d.drawImage(deck.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
			g2d.drawImage(getRace().getIcon(), 10, 12, null);

			g2d.setFont(new Font("Arial", Font.BOLD, 20));
			g2d.setColor(deck.getFrame().getPrimaryColor());
			Graph.drawOutlinedString(g2d, StringUtils.abbreviate(card.getName(), MAX_NAME_LENGTH), 38, 30, 3, deck.getFrame().getBackgroundColor());

			if (!desc.isEmpty()) {
				g2d.setColor(deck.getFrame().getSecondaryColor());
				g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 12));
				List<String> tags = getTags();
				if (tags.size() > 4) {
					tags = tags.subList(0, 3);
					tags.add("...");
				}
				g2d.drawString(tags.stream().map(locale::get).map(String::toUpperCase).toList().toString(), 7, 275);

				Font normal = Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 10);
				Font dynamic = Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 8);
				Graph.drawMultilineString(g2d,
						StringUtils.abbreviate(desc, MAX_DESC_LENGTH), 7, 287, 211, 3,
						s -> {
							String str = Utils.extract(s, "\\{(\\d+)}", 1);

							if (str != null) {
								double val = Double.parseDouble(str);

								g2d.setFont(dynamic);
								g2d.setColor(Color.ORANGE);
								return "\u200B" + s.replaceFirst("\\{\\d+}", Utils.roundToString(val * (1 + stats.getPower()), 2));
							}

							g2d.setFont(normal);
							g2d.setColor(deck.getFrame().getSecondaryColor());
							return s;
						},
						(str, x, y) -> {
							if (str.startsWith("\u200B")) {
								Graph.drawOutlinedString(g2d, str.substring(1), x, y, 2, Color.BLACK);
							} else {
								g2d.drawString(str, x, y);
							}
						}
				);
			}

			drawCosts(g2d);
			if (!isSupporting()) {
				drawAttributes(g2d, !desc.isEmpty());
			}

			if (!isAvailable()) {
				RescaleOp op = new RescaleOp(0.5f, 0, null);
				op.filter(out, out);
			}

			if (isDefending()) {
				g2d.drawImage(IO.getResourceAsImage("shoukan/states/defense.png"), 0, 0, null);
			}

			g2d.dispose();

			cache = new Pair<>(hash, out);
		}

		return cache.getSecond();
	}

	@Override
	public int renderHashCode(I18N locale) {
		return Objects.hash(equipments, stats, state, hand, locale, isSupporting());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Senshi senshi = (Senshi) o;
		return Objects.equals(id, senshi.id) && Objects.equals(card, senshi.card) && race == senshi.race;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card, race);
	}

	@Override
	public Senshi clone() throws CloneNotSupportedException {
		return (Senshi) super.clone();
	}

	@Override
	public String toString() {
		return card.getName();
	}
}
