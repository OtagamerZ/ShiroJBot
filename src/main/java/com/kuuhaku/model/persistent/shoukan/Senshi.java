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
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedLinkedList;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.*;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "senshi")
public class Senshi extends DAO<Senshi> implements EffectHolder<Senshi> {
	@Transient
	public final String KLASS = getClass().getName();
	public transient long SERIAL = Constants.DEFAULT_RNG.nextLong();

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

	@Transient
	private transient BondedLinkedList<Evogear> equipments = new BondedLinkedList<>(Objects::nonNull, e -> {
		e.setEquipper(this);
		e.setHand(getHand());
		getHand().getGame().trigger(Trigger.ON_EQUIP, asSource(Trigger.ON_EQUIP));
	});
	private transient CardExtra stats = new CardExtra();
	private transient SlotColumn slot = null;
	private transient Hand hand = null;
	private transient Hand leech = null;

	@Transient
	private int state = 0b10;
	/*
	0x00 F FFF FF
	     │ │││ └┴ 0011 1111
	     │ │││      ││ │││└ solid
	     │ │││      ││ ││└─ available
	     │ │││      ││ │└── defending
	     │ │││      ││ └─── flipped
	     │ │││      │└ special summon
	     │ │││      └─ sealed
	     │ ││└─ (0 - 15) sleeping
	     │ │└── (0 - 15) stunned
	     │ └─── (0 - 15) stasis
	     └ (0 - 15) cooldown
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

	@Override
	public List<String> getTags() {
		List<String> out = new ArrayList<>();
		if (race != Race.NONE) {
			out.add("race/" + race.name());
		}
		if (hasEffect()) {
			if (base.getTags().contains("MATERIAL")) {
				out.add("tag/base");
			} else {
				out.add("tag/effect");
			}
		} else if (isSealed()) {
			out.add("tag/sealed");
		}

		for (Object tag : base.getTags()) {
			if (out.contains("tag/base") && tag.equals("MATERIAL")) continue;

			out.add("tag/" + tag);
		}

		return out;
	}

	public List<Evogear> getEquipments() {
		equipments.removeIf(e -> !equals(e.getEquipper()));

		while (equipments.size() > 3) {
			hand.getGraveyard().add(equipments.removeFirst());
		}

		return equipments;
	}

	public boolean isEquipped(String id) {
		return equipments.stream().anyMatch(e -> e.getCard().getId().equals(id));
	}

	@Override
	public SlotColumn getSlot() {
		return Utils.getOr(slot, new SlotColumn(hand.getGame(), hand.getSide(), -1));
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

	public Hand getLeech() {
		return leech;
	}

	public void setLeech(Hand leech) {
		if (this.leech != null) {
			if (leech == null) {
				this.leech.getLeeches().remove(this);
			} else {
				return;
			}
		}

		this.leech = leech;
		if (this.leech != null) {
			this.leech.getLeeches().add(this);
		}
	}

	@Override
	public String getDescription(I18N locale) {
		return Utils.getOr(stats.getDescription(locale), base.getDescription(locale));
	}

	@Override
	public int getMPCost() {
		return Math.max(0, base.getMana() + stats.getMana()) + (isFusion() ? 5 : 0);
	}

	@Override
	public int getHPCost() {
		return Math.max(0, base.getBlood() + stats.getBlood());
	}

	@Override
	public int getSCCost() {
		return Math.max(0, base.getSacrifices() + stats.getSacrifices());
	}

	@Override
	public int getDmg() {
		int sum = base.getAtk() + stats.getAtk() + getEquipDmg();

		double mult = 1;
		if (hand != null) {
			if (hand.getOrigin().minor() == Race.UNDEAD) {
				mult *= 1 + (hand.getGraveyard().size() * 0.005);
			}

			if (hand.isLowLife() && hand.getOrigin().synergy() == Race.ONI) {
				mult *= 1.02;
			} else if (hand.getHPPrcnt() > 1 && hand.getOrigin().synergy() == Race.GHOUL) {
				mult *= 1.05;
			}

			mult *= getFieldMult(hand.getGame().getArena().getField());
		}
		if (isStunned()) {
			mult /= 2;
		}

		return (int) Math.max(0, sum * mult * getAttrMult());
	}

	@Override
	public int getDef() {
		int sum = base.getDef() + stats.getDef() + getEquipDef();

		double mult = 1;
		if (hand != null) {
			if (hand.getOrigin().minor() == Race.SPIRIT) {
				mult *= 1 + (hand.getGraveyard().size() * 0.01);
			}

			mult *= getFieldMult(hand.getGame().getArena().getField());
		}
		if (isStunned()) {
			mult /= 2;
		}

		return (int) Math.max(0, sum * mult * getAttrMult());
	}

	public double getFieldMult(Field f) {
		if (stats.hasFlag(Flag.IGNORE_FIELD)) return 1;

		double mult = 1;
		Race[] races = race.split();
		for (Race r : races) {
			double mod = f.getModifiers().getDouble(r.name()) / races.length;
			mult += mod;

			if (mod != 0 && hand.getOrigin().synergy() == Race.ELF) {
				mult += 0.05;
			}
		}

		return mult;
	}

	public int getActiveAttr() {
		if (isDefending()) return getDef();
		return getDmg();
	}

	public int getActiveAttr(boolean dbl) {
		if (isDefending()) {
			if (dbl) {
				return getDef() * 2;
			}

			return getDef();
		}

		return getDmg();
	}

	@Override
	public int getDodge() {
		int sum = (int) ((base.getDodge() + stats.getDodge() + getEquipDodge()) * getAttrMult());
		if (hand != null && hand.getGame().getArena().getField().getType() == FieldType.DUNGEON) {
			sum = Math.min(sum, 50);
		}

		return Math.max(0, sum);
	}

	@Override
	public int getBlock() {
		int sum = base.getBlock() + stats.getBlock() + getEquipBlock();

		int min = 0;
		if (hand != null && hand.getOrigin().synergy() == Race.CYBORG) {
			min += 2;
		}

		return (int) Math.max(0, (min + sum) * getAttrMult());
	}

	private double getAttrMult() {
		double mult = 1;
		if (hand != null && hand.getOrigin().minor() == Race.NONE) {
			if (race == hand.getOrigin().major()) {
				mult *= 1.25;
			} else {
				mult *= 0.5;
			}
		}
		mult *= stats.getAttrMult();

		return mult;
	}

	public int getEquipDmg() {
		if (stats.hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getDmg).sum();
	}

	public int getEquipDef() {
		if (stats.hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getDef).sum();
	}

	public int getEquipDodge() {
		if (stats.hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getDodge).sum();
	}

	public int getEquipBlock() {
		if (stats.hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getBlock).sum();
	}

	public int getActiveEquips() {
		try {
			if (isDefending()) return getEquipDef();
			return getEquipDmg();
		} finally {
			stats.popFlag(Flag.NO_EQUIP);
		}
	}

	public int getActiveEquips(boolean dbl) {
		try {
			if (isDefending()) {
				if (dbl) {
					return getEquipDef() * 2;
				}

				return getEquipDef();
			}

			return getEquipDmg();
		} finally {
			stats.popFlag(Flag.NO_EQUIP);
		}
	}

	@Override
	public boolean isSolid() {
		return Bit.on(state, 0) && !base.getTags().contains("FUSION");
	}

	@Override
	public void setSolid(boolean solid) {
		state = Bit.set(state, 0, solid);
	}

	@Override
	public boolean isAvailable() {
		return Bit.on(state, 1) && !isStasis() && !isStunned() && !isSleeping();
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

		if (!isFlipped()) {
			hand.getGame().trigger(Trigger.ON_SWITCH, asSource(Trigger.ON_SWITCH));
		}
	}

	public boolean canAttack() {
		return !isDefending() && isAvailable();
	}

	@Override
	public boolean isFlipped() {
		return Bit.on(state, 3);
	}

	@Override
	public void setFlipped(boolean flipped) {
		if (isFlipped() && !flipped) {
			setDefending(true);

			if (hand != null) {
				if (hand.getGame().getCurrentSide() != hand.getSide()) {
					hand.getGame().trigger(Trigger.ON_FLIP, asSource(Trigger.ON_FLIP));
				} else {
					hand.getGame().trigger(Trigger.ON_SUMMON, asSource(Trigger.ON_SUMMON));
				}
			}
		}

		state = Bit.set(state, 3, flipped);
	}

	@Override
	public boolean isSPSummon() {
		return Bit.on(state, 4);
	}

	@Override
	public void setSPSummon(boolean special) {
		state = Bit.set(state, 4, special);
	}

	public boolean isSealed() {
		return Bit.on(state, 5);
	}

	public void setSealed(boolean sealed) {
		state = Bit.set(state, 5, sealed);
	}

	public boolean isSleeping() {
		return !isStunned() && Bit.on(state, 2, 4);
	}

	public void setSleep(int time) {
		int curr = Bit.get(state, 2, 4);
		state = Bit.set(state, 2, Math.max(curr, time), 4);
	}

	public void reduceSleep(int time) {
		int curr = Bit.get(state, 2, 4);
		state = Bit.set(state, 2, Math.max(0, curr - time), 4);
	}

	public boolean isStunned() {
		return !isStasis() && Bit.on(state, 3, 4);
	}

	public void setStun(int time) {
		int curr = Bit.get(state, 3, 4);
		state = Bit.set(state, 3, Math.max(curr, time), 4);
	}

	public void reduceStun(int time) {
		int curr = Bit.get(state, 3, 4);
		state = Bit.set(state, 3, Math.max(0, curr - time), 4);
	}

	public boolean isStasis() {
		return Bit.on(state, 4, 4);
	}

	public void setStasis(int time) {
		int curr = Bit.get(state, 4, 4);
		state = Bit.set(state, 4, Math.max(curr, time), 4);
	}

	public void reduceStasis(int time) {
		int curr = Bit.get(state, 4, 4);
		state = Bit.set(state, 4, Math.max(0, curr - time), 4);
	}

	@Override
	public int getCooldown() {
		return Bit.get(state, 5, 4);
	}

	@Override
	public void setCooldown(int time) {
		int curr = Bit.get(state, 5, 4);
		state = Bit.set(state, 5, Math.max(curr, time), 4);
	}

	public void reduceCooldown(int time) {
		int curr = Bit.get(state, 5, 4);
		state = Bit.set(state, 5, Math.max(0, curr - time), 4);
	}

	@Override
	public List<String> getCurses() {
		return stats.getCurses();
	}

	public CardState getState() {
		if (isFlipped()) return CardState.FLIPPED;
		else if (isDefending()) return CardState.DEFENSE;

		return CardState.ATTACK;
	}

	public boolean isFusion() {
		return base.getTags().contains("FUSION");
	}

	public boolean isBlinded() {
		return isBlinded(false);
	}

	public boolean isBlinded(boolean pop) {
		if (hand != null && hand.getGame().getArena().getField().getType() == FieldType.NIGHT) {
			return true;
		}

		return pop ? stats.popFlag(Flag.BLIND) : stats.hasFlag(Flag.BLIND);
	}

	public boolean isSupporting() {
		return slot != null && slot.hasBottom() && slot.getBottom().SERIAL == SERIAL;
	}

	public Senshi getFrontline() {
		if (slot == null || !isSupporting()) return null;

		return slot.getTop();
	}

	public Senshi getSupport() {
		if (slot == null || isSupporting()) return null;

		return slot.getBottom();
	}

	public String getEffect() {
		return Utils.getOr(stats.getEffect(), base.getEffect());
	}

	public boolean hasEffect() {
		return !isSealed() && getEffect().contains(Trigger.class.getName());
	}

	@Override
	public boolean execute(EffectParameters ep) {
		if (isStunned() || stats.popFlag(Flag.NO_EFFECT) || hand.getLockTime(Lock.EFFECT) > 0) return false;

		Trigger trigger;
		check:
		if (equals(ep.source().card())) {
			trigger = ep.source().trigger();
		} else {
			for (Target target : ep.targets()) {
				if (equals(target.card())) {
					trigger = target.trigger();
					break check;
				}
			}

			trigger = ep.trigger();
		}

		if (base.isLocked()) return false;
		else if (ep.size() == 0 && trigger == Trigger.DEFER) return false;
		else if (trigger == Trigger.ACTIVATE && (getCooldown() > 0 || isSupporting())) return false;

		//Hand other = ep.getHands().get(ep.getOtherSide());
		try {
			base.lock();

			/*if (hero != null) {
				other.setHeroDefense(true);
			}*/

			if (!hasEffect()) {
				Utils.exec(getEffect(), Map.of(
						"ep", ep,
						"self", this,
						"trigger", trigger,
						"game", hand.getGame(),
						"side", hand.getSide()
				));
			}

			for (Evogear e : equipments) {
				e.execute(new EffectParameters(trigger, ep.source(), ep.targets()));
			}

			Senshi sup = getSupport();
			if (sup != null) {
				sup.execute(new EffectParameters(Trigger.DEFER, ep.source(), ep.targets()));
			}

			for (@Language("Groovy") String curse : stats.getCurses()) {
				if (curse.isBlank() || !curse.contains(ep.trigger().name())) continue;

				Utils.exec(curse, Map.of(
						"ep", ep,
						"self", this,
						"trigger", trigger,
						"game", hand.getGame(),
						"side", hand.getSide()
				));
			}

			return true;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute " + card.getName() + " effect", e);
			return false;
		} finally {
			unlock();
			//other.setHeroDefense(false);
		}
	}

	public void unlock() {
		base.unlock();
		for (Evogear e : equipments) {
			e.getBase().unlock();
		}
	}

	@Override
	public void reset() {
		equipments = new BondedLinkedList<>(Objects::nonNull, e -> {
			e.setEquipper(this);
			e.setHand(getHand());
			getHand().getGame().trigger(Trigger.ON_EQUIP, asSource(Trigger.ON_EQUIP));
		});
		stats = new CardExtra();
		slot = null;
		if (leech != null) {
			leech.getLeeches().remove(this);
		}

		byte base = 0b10;
		base = (byte) Bit.set(base, 0, isSolid());
		base = (byte) Bit.set(base, 4, isSPSummon());
		base = (byte) Bit.set(base, 5, isSealed());

		state = base;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		if (isFlipped()) return deck.getFrame().getBack(deck);

		String desc = getDescription(locale);

		BufferedImage img = getVanity().drawCardNoBorder(deck.isUsingChrome());
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		g2d.setClip(deck.getFrame().getBoundary());
		g2d.drawImage(img, 0, 0, null);
		g2d.setClip(null);

		g2d.drawImage(deck.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
		g2d.drawImage(getRace().getIcon(), 190, 12, null);

		g2d.setFont(FONT);
		g2d.setColor(deck.getFrame().getPrimaryColor());
		String name = StringUtils.abbreviate(card.getName(), MAX_NAME_LENGTH);
		Graph.drawOutlinedString(g2d, name, 12, 30, 2, deck.getFrame().getBackgroundColor());

		if (!desc.isEmpty()) {
			g2d.setColor(deck.getFrame().getSecondaryColor());
			g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 11));
			g2d.drawString(processTags(locale), 7, 275);

			Graph.drawMultilineString(g2d, desc,
					7, 287, 211, 2,
					parseValues(g2d, deck, this), highlightValues(g2d)
			);
		}

		if (!stats.getWrite().isBlank()) {
			String val = String.valueOf(stats.getWrite());
			g2d.setColor(Color.ORANGE);
			g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 18));
			Graph.drawOutlinedString(g2d, val, 25, 51 + (23 + g2d.getFontMetrics().getHeight()) / 2, 2, Color.BLACK);
		}

		drawCosts(g2d);
		if (!isSupporting() && !stats.hasFlag(Flag.HIDE_STATS)) {
			drawAttributes(g2d, !desc.isEmpty());
		}

		if (!isAvailable()) {
			RescaleOp op = new RescaleOp(0.5f, 0, null);
			op.filter(out, out);
		}

		if (isStasis()) {
			g2d.drawImage(IO.getResourceAsImage("shoukan/states/stasis.png"), 0, 0, null);
		} else if (isStunned()) {
			g2d.drawImage(IO.getResourceAsImage("shoukan/states/stun.png"), 0, 0, null);
		} else if (isSleeping()) {
			g2d.drawImage(IO.getResourceAsImage("shoukan/states/sleep.png"), 0, 0, null);
		} else if (isDefending()) {
			g2d.drawImage(IO.getResourceAsImage("shoukan/states/defense.png"), 0, 0, null);
		}

		g2d.dispose();

		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Senshi senshi = (Senshi) o;
		return Objects.equals(id, senshi.id)
				&& Objects.equals(card, senshi.card)
				&& Objects.equals(slot, senshi.slot)
				&& race == senshi.race
				&& SERIAL == senshi.SERIAL;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card, slot, race);
	}

	@Override
	public Senshi clone() throws CloneNotSupportedException {
		return (Senshi) super.clone();
	}

	@Override
	public String toString() {
		return card.getName();
	}

	public static Senshi getRandom() {
		String id = DAO.queryNative(String.class, "SELECT card_id FROM senshi ORDER BY RANDOM()");
		if (id == null) return null;

		return DAO.find(Senshi.class, id);
	}

	public static Senshi getRandom(String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM senshi");
		for (String f : filters) {
			query.appendNewLine(f);
		}
		query.appendNewLine("ORDER BY RANDOM()");

		String id = DAO.queryNative(String.class, query.toString());
		if (id == null) return null;

		return DAO.find(Senshi.class, id);
	}
}
