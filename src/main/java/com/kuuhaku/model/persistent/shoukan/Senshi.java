/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.exceptions.SelectionException;
import com.kuuhaku.exceptions.TargetException;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.interfaces.shoukan.Proxy;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.CachedScriptManager;
import com.kuuhaku.model.common.XList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.shoukan.*;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.shoukan.DeferredTrigger;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.*;
import jakarta.persistence.*;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "senshi", schema = "kawaipon")
public class Senshi extends DAO<Senshi> implements EffectHolder<Senshi> {
	@Transient
	public final String KLASS = getClass().getName();
	@Transient
	public final long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Enumerated(EnumType.STRING)
	@Column(name = "race", nullable = false)
	private Race race;

	@Embedded
	private CardAttributes base = new CardAttributes();

	@Transient
	private final transient BondedList<Evogear> equipments = new BondedList<>((e, it) -> {
		if (getEquipments(false).contains(e)) return false;

		e.setEquipper(this);
		e.setHand(getHand());
		e.executeAssert(ON_INITIALIZE);

		Shoukan game = getGame();
		getHand().getData().put("last_equipment", e);
		getHand().getData().put("last_evogear", e);

		if (e.hasCharm(Charm.TIMEWARP)) {
			int times = Charm.TIMEWARP.getValue(e.getTier());
			for (int i = 0; i < times; i++) {
				getStats().getPower().set(e, -0.1 * (i + 1));
				game.trigger(ON_TURN_BEGIN, asSource(ON_TURN_BEGIN));
				game.trigger(ON_TURN_END, asSource(ON_TURN_END));
			}

			getStats().getPower().set(e, 0);
		}

		if (e.hasCharm(Charm.CLONE)) {
			game.putAtOpenSlot(getSide(), true, withCopy(s -> {
				s.getStats().getAttrMult().set(-1 + (0.25 * e.getTier()));
				s.getStats().getData().put("cloned", true);

				if (s.hasAbility()) {
					s.setCooldown(1);
				}
			}));
		}

		return true;
	}, e -> {
		e.setCurrentStack(getEquipments(false));
		getGame().trigger(ON_EQUIP, asSource(ON_EQUIP));
	}, e -> {
		e.executeAssert(ON_REMOVE);
		e.setEquipper(null);
	});
	private transient CardExtra stats = new CardExtra();
	private transient SlotColumn slot = null;
	private transient Hand hand = null;
	private transient Senshi target = null;
	private transient Senshi lastInteraction = null;
	private final transient CachedScriptManager cachedEffect = new CachedScriptManager();
	private final transient Set<Drawable<?>> blocked = new HashSet<>();
	private transient TargetType targetType = TargetType.NONE;
	private transient ElementType element = null;
	private transient StashedCard stashRef = null;
	private transient BondedList<?> currentStack;
	private transient Trigger currentTrigger = null;

	@Transient
	private long state = 0b1;
	/*
	0x0000 000 FF FFFFF FF
	           ││ │││││ └┴ 0 111 1111
	           ││ │││││      │││ │││└─ available
	           ││ │││││      │││ ││└── defending
	           ││ │││││      │││ │└─── flipped
	           ││ │││││      │││ └──── sealed
	           ││ │││││      ││└─ ethereal
	           ││ │││││      │└── switched
	           ││ │││││      └─── manipulated
	           ││ ││││└─ (0 - 15) sleeping
	           ││ │││└── (0 - 15) stunned
	           ││ ││└─── (0 - 15) stasis
	           ││ │└──── (0 - 15) taunt
	           ││ └───── (0 - 15) berserk
	           │└ (0 - 15) cooldown
	           └─ (0 - 15) attacks
	 */

	public Senshi() {
	}

	public Senshi(Actor actor, I18N locale) {
		this.card = new Card(actor, locale);
		this.id = card.getId();
		this.race = actor.getRace();
		this.base = new CardAttributes();
		this.stats = new CardExtra();
	}

	public Senshi(String id, Card card, Race race, CardAttributes base, CardExtra stats, StashedCard stashRef) {
		this.id = id;
		this.card = card;
		this.race = race;
		this.base = base;
		this.stats = stats;
		this.stashRef = stashRef;
	}

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

	@Override
	public Card getVanity() {
		return Utils.getOr(stats.getVanity(), card);
	}

	public Race getRace() {
		if (hand != null && getGame() != null && hand.getOrigins().synergy() == Race.DOPPELGANGER) {
			return Race.MIXED;
		}

		return Utils.getOr(stats.getRace(), race);
	}

	@Override
	public CardAttributes getBase() {
		return base;
	}

	@Override
	public CardExtra getStats() {
		return stats;
	}

	@Override
	public boolean hasFlag(Flag flag) {
		return hasFlag(flag, false);
	}

	@Override
	public boolean hasFlag(Flag flag, boolean pop) {
		for (Evogear e : equipments) {
			if (e.hasFlag(flag, pop)) return true;
		}

		if (pop) return stats.getFlags().pop(flag);
		else return stats.getFlags().has(flag);
	}

	@Override
	public TagBundle getTagBundle() {
		TagBundle out = new TagBundle();
		if (race != Race.NONE) {
			out.add("race", getRace().name());
		}

		if (hasEffect()) {
			if (base.getTags().contains("MATERIAL")) {
				out.add("tag", "base");
			} else {
				out.add("tag", "effect");
			}
		} else if (isSealed()) {
			out.add("tag", "sealed");
		}

		out.add("element", getElement().name().toLowerCase());

		List<String> tags = base.getTags().stream()
				.map(String::valueOf)
				.sorted()
				.toList();

		for (String tag : tags) {
			if (out.contains("base") && tag.equals("MATERIAL")) continue;

			out.add("tag", tag.toLowerCase());
		}

		return out;
	}

	public BondedList<Evogear> getEquipments() {
		return getEquipments(true);
	}

	public BondedList<Evogear> getEquipments(boolean sweep) {
		if (sweep) {
			equipments.removeIf(e -> !equals(e.getEquipper()));
		}

		if (equipments.size() > 3) {
			int fixs = (int) equipments.stream().filter(EffectHolder::isFixed).count();

			List<Evogear> removed = new ArrayList<>();
			Iterator<Evogear> it = equipments.iterator();
			while (equipments.size() > 3 && it.hasNext()) {
				Evogear e = it.next();
				if (!e.isFixed() || fixs > 3) {
					removed.add(e);
					it.remove();

					if (e.isFixed()) {
						fixs--;
					}
				}
			}

			hand.getGraveyard().addAll(removed);
		}

		return equipments;
	}

	public boolean isEquipped(String id) {
		return equipments.stream().anyMatch(e -> e.getCard().getId().equals(id));
	}

	public Evogear getEquipped(String id) {
		return equipments.stream()
				.filter(e -> e.getCard().getId().equals(id))
				.findFirst()
				.orElse(null);
	}

	@Override
	public boolean hasCharm(Charm charm) {
		return hasCharm(charm, false);
	}

	public boolean hasCharm(Charm charm, boolean pop) {
		if (isSupporting()) return false;

		for (Evogear e : equipments) {
			if (e.hasCharm(charm)) {
				if (pop && Utils.equalsAny(charm, Charm.SHIELD, Charm.WARDING)) {
					int charges = e.getStats().getData().getInt("C_" + charm.name(), 0) + 1;
					if (charges >= charm.getValue(e.getTier())) {
						e.getCharms().remove(charm.name());
					} else {
						e.getStats().getData().put("C_" + charm.name(), charges);
					}
				}

				return true;
			}
		}

		return false;
	}

	public Evogear unequip(Evogear evo) {
		Iterator<Evogear> it = equipments.iterator();
		while (it.hasNext()) {
			Evogear e = it.next();

			if (e.equals(evo)) {
				it.remove();
				hand.getGraveyard().add(e);
				return e;
			}
		}

		return null;
	}

	public Evogear unequip(String id) {
		Iterator<Evogear> it = equipments.iterator();
		while (it.hasNext()) {
			Evogear e = it.next();

			if (e.getCard().getId().equals(id)) {
				it.remove();
				hand.getGraveyard().add(e);
				return e;
			}
		}

		return null;
	}

	@Override
	public SlotColumn getSlot() {
		return Utils.getOr(slot, new SlotColumn(getGame(), hand.getSide(), -1));
	}

	@Override
	public void setSlot(SlotColumn slot) {
		this.slot = slot;
	}

	public void replace(Senshi other) {
		getSlot().replace(this, other);
	}

	public void augment(Senshi other) {
		replace(new AugmentSenshi(other, this));
	}

	public void swap(Senshi other) {
		getSlot().swap(this, other);
	}

	public Senshi getLeft() {
		SlotColumn slt = getSlot().getLeft();
		if (slt == null) return null;

		if (isSupporting()) {
			return slt.getBottom();
		} else {
			return slt.getTop();
		}
	}

	public void setLeft(Senshi card) {
		SlotColumn slt = getSlot().getLeft();
		if (slt == null) return;

		if (isSupporting()) {
			slt.setBottom(card);
		} else {
			slt.setTop(card);
		}
	}

	public Senshi getRight() {
		SlotColumn slt = getSlot().getRight();
		if (slt == null) return null;

		if (isSupporting()) {
			return slt.getBottom();
		} else {
			return slt.getTop();
		}
	}

	public void setRight(Senshi card) {
		SlotColumn slt = getSlot().getRight();
		if (slt == null) return;

		if (isSupporting()) {
			slt.setBottom(card);
		} else {
			slt.setTop(card);
		}
	}

	public Senshi getFront() {
		return getFront((Boolean) null);
	}

	public Senshi getFront(Boolean top) {
		List<Senshi> tgts = getFront(top, getIndex());
		if (tgts.isEmpty()) return null;

		return tgts.getFirst();
	}

	public List<Senshi> getFront(int... indexes) {
		return getFront(null, indexes);
	}

	public List<Senshi> getFront(Boolean top, int... indexes) {
		return getCards(getSide().getOther(), top, indexes);
	}

	public List<Senshi> getAllies() {
		return getCards(getSide());
	}

	public List<Senshi> getAllies(int... indexes) {
		return getAllies(null, indexes);
	}

	public List<Senshi> getAllies(Boolean top, int... indexes) {
		return getCards(getSide(), top, indexes);
	}

	public List<Senshi> getNearby() {
		if (slot == null) return List.of();

		List<Senshi> out = new ArrayList<>();

		if (getLeft() != null) {
			out.add(getLeft());
		}

		if (getFrontline() != null) {
			out.add(getFrontline());
		}

		if (getSupport() != null) {
			out.add(getSupport());
		}

		if (getRight() != null) {
			out.add(getRight());
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

		if (this instanceof Proxy<?> p) {
			p.getOriginal().setHand(hand);
		}
	}

	@Override
	public String getDescription(I18N locale) {
		EffectHolder<?> source = getSource();
		String out = Utils.getOr(source.getStats().getDescription(locale), source.getBase().getDescription(locale));
		if (hand != null) {
			if (hand.getOrigins().major() == Race.DEMON) {
				out = out.replace("$mp", "($hp/($bhp*0.08))");
			}
		}

		return out;
	}

	@Override
	public int getMPCost(boolean ignoreRace) {
		int cost = Math.max(0, Calc.round((base.getMana() + stats.getMana().get() + (isFusion() ? 5 : 0)) * getCostMult()));
		if (hand != null && !ignoreRace) {
			if (hand.getOrigins().synergy() == Race.CELESTIAL) {
				cost = hand.getUserDeck().getAverageMPCost();
			}

			if (hand.getOrigins().synergy() == Race.HOMUNCULUS && cost > hand.getMP()) {
				cost = hand.getMP();
			}

			if (hand.getOrigins().major() == Race.DEMON) {
				cost = 0;
			}
		}

		return cost;
	}

	@Override
	public int getHPCost(boolean ignoreRace) {
		int cost = Math.max(0, Calc.round((base.getBlood() + stats.getBlood().get()) * getCostMult()));
		if (hand != null) {
			if (!ignoreRace) {
				if (hand.getOrigins().synergy() == Race.CELESTIAL) {
					cost = hand.getUserDeck().getAverageHPCost();
				}
			}

			int mp = getMPCost(true);

			if (hand.getOrigins().major() == Race.DEMON) {
				cost += (int) (hand.getBase().hp() * 0.08 * mp);
			}
		}

		return cost;
	}

	@Override
	public int getSCCost() {
		int cost = Math.max(0, Calc.round((base.getSacrifices() + stats.getSacrifices().get()) * getCostMult()));
		if (hand != null) {
			int mp = getMPCost(true);

			if (hand.getOrigins().synergy() == Race.HOMUNCULUS && mp > hand.getMP()) {
				cost += mp - hand.getMP();
			}
		}

		return cost;
	}

	@Override
	public int getDmg() {
		int sum = base.getAtk() + (int) stats.getAtk().get() + getEquipDmg();

		double mult = stats.getAtkMult().get();
		if (hand != null) {
			switch (hand.getOrigins().synergy()) {
				case ONI -> {
					if (hand.isLowLife()) {
						mult *= 1.2;
					}
				}
				case CYBERBEAST -> {
					if (getGame() != null) {
						sum += getGame().getCards(getSide()).stream().mapToInt(Senshi::getParry).sum();
					}
				}
			}

			mult *= getFieldMult();
		}

		if (isStunned()) {
			mult /= 2;
		}

		return Math.max(0, Calc.round(sum * mult * getAttrMult()));
	}

	@Override
	public int getDfs() {
		int sum = base.getDfs() + (int) stats.getDfs().get() + getEquipDfs();

		double mult = stats.getDfsMult().get();
		if (hand != null) {
			mult *= getFieldMult();

			if (hand.getOrigins().synergy() == Race.WEREBEAST && isSleeping()) {
				mult *= 1.5;
			}
		}

		if (isStunned()) {
			mult /= 2;
		}

		return Math.max(0, Calc.round(sum * mult * getAttrMult()));
	}

	public double getFieldMult() {
		if (getGame() == null || hasFlag(Flag.IGNORE_FIELD)) return 1;
		Field f = getGame().getArena().getField();

		double mult = 1;
		int races = getRace().split(true).size();
		for (Map.Entry<String, Object> e : f.getModifiers().entrySet()) {
			Race r = Race.valueOf(e.getKey());

			if (getRace().isRace(r)) {
				double mod = ((Number) e.getValue()).doubleValue();

				if (mod != 0 && hand.getOrigins().synergy() == Race.ELF) {
					mod += 0.15;
				}

				if (mod != 0 && hand.getOther().getOrigins().synergy() == Race.DARK_ELF) {
					mod -= 0.15;
				}

				mult += mod / races;
			}
		}

		return mult;
	}

	public int getActiveAttr() {
		if (isDefending()) return getDfs();
		return getDmg();
	}

	public int getActiveAttr(boolean dbl) {
		if (isDefending()) {
			if (dbl) {
				return getDfs() * 2;
			}

			return getDfs();
		}

		return getDmg();
	}

	@Override
	public int getDodge() {
		if (isSleeping() || isStunned()) return 0;

		int min = 0;
		int sum = base.getDodge() + (int) stats.getDodge().get() + getEquipDodge();

		if (isBlinded()) {
			sum /= 2;
		}

		if (hand != null) {
			if (hand.getOrigins().synergy() == Race.GEIST) {
				min += 10;
			}

			if (hand.getOrigins().synergy() == Race.ELEMENTAL) {
				int wind = (int) getCards(hand.getSide()).parallelStream()
						.filter(s -> s.getElement() == ElementType.WIND)
						.count();

				if (wind >= 4) {
					sum += 20 * getNearby().size();
				}
			}

			if (getGame() != null && getGame().getArena().getField().getType() == FieldType.DUNGEON) {
				return Utils.clamp(sum, min, 50);
			}
		}

		return Utils.clamp(min + sum, min, 100);
	}

	@Override
	public int getParry() {
		int sum = base.getParry() + (int) stats.getParry().get() + getEquipParry();

		int min = 0;
		if (hand != null) {
			if (hand.getOrigins().synergy() == Race.CYBORG) {
				min += 10;
			}
		}

		return Utils.clamp(min + sum, min, 100);
	}

	@Override
	public double getCostMult() {
		double mult = stats.getCostMult().get();
		if (hand != null) {
			if (hand.getOrigins().synergy() == Race.PIXIE) {
				mult *= getFieldMult();
			}

			if (hand.getOrigins().synergy() == Race.DULLAHAN) {
				mult *= 2;
			}
		}

		return mult;
	}

	@Override
	public double getAttrMult() {
		double mult = stats.getAttrMult().get();
		if (hand != null) {
			if (hand.getOrigins().isPure() && race != hand.getOrigins().major()) {
				mult *= 0.5;
			}

			if (getGame() != null && getGame().getArcade() == Arcade.OVERCHARGE) {
				mult *= 1.5;
			}

			if (hand.getOrigins().synergy() == Race.REVENANT && !hasEffect()) {
				mult *= 1.2;
			} else if (hand.getOrigins().synergy() == Race.FABLED) {
				mult *= getPower();
			}
		}

		if (stashRef != null) {
			mult *= 1 + stashRef.getQuality() / 100;
		}

		return mult;
	}

	@Override
	public double getPower() {
		double mult = stats.getPower().get() * (hasFlag(Flag.EMPOWERED) ? 1.5 : 1);
		if (hand != null) {
			if (hand.getOrigins().major() == Race.MIXED) {
				mult *= 1 - 0.07 * hand.getOrigins().minor().length;
			}

			if (getGame() != null && getGame().getArcade() == Arcade.OVERCHARGE) {
				mult *= 1.75;
			}

			if (isSupporting()) {
				if (hand.getOrigins().synergy() == Race.FAERIE) {
					mult *= 1.25;
				} else {
					mult *= 0.75;
				}
			}

			if (hand.getOrigins().hasMinor(Race.SPIRIT)) {
				mult *= getFieldMult();
			}

			if (hand.getOrigins().synergy() == Race.DRYAD) {
				mult *= 1 + Math.max(0, hand.getRegDeg().peek() / hand.getBase().hp());
			} else if (hand.getOrigins().synergy() == Race.ALIEN) {
				mult *= 1 + Calc.prcnt(hand.getUserDeck().getEvoWeight(), 24) / 4;
			}
		}

		return mult;
	}

	private int getEquipValue(ToIntFunction<EffectHolder<?>> extractor) {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		int sum = equipments.stream().filter(Evogear::isAvailable).mapToInt(extractor).sum();
		if (hand != null) {
			if (Utils.equalsAny(Race.SLIME, hand.getOrigins().synergy(), hand.getOther().getOrigins().synergy())) {
				return 0;
			} else if (hand.getOrigins().synergy() == Race.EX_MACHINA) {
				Senshi sup = getSupport();
				if (sup != null) {
					sum += extractor.applyAsInt(sup);
				}
			}
		}

		return sum;
	}

	public int getEquipDmg() {
		return getEquipValue(EffectHolder::getDmg);
	}

	public int getEquipDfs() {
		return getEquipValue(EffectHolder::getDfs);
	}

	public int getEquipDodge() {
		return getEquipValue(EffectHolder::getDodge);
	}

	public int getEquipParry() {
		return getEquipValue(EffectHolder::getParry);
	}

	public int getActiveEquips() {
		try {
			if (isDefending()) return getEquipDfs();
			return getEquipDmg();
		} finally {
			hasFlag(Flag.NO_EQUIP, true);
		}
	}

	public int getActiveEquips(boolean dbl) {
		try {
			if (isDefending()) {
				if (dbl) {
					return getEquipDfs() * 2;
				}

				return getEquipDfs();
			}

			return getEquipDmg();
		} finally {
			hasFlag(Flag.NO_EQUIP, true);
		}
	}

	@Override
	public boolean isAvailable() {
		return Bit64.on(state, 0) && !isStasis() && !isStunned() && !isSleeping();
	}

	@Override
	public void setAvailable(boolean available) {
		boolean was = isAvailable();
		state = Bit64.set(state, 0, available);

		if (!was && isAvailable()) {
			resetAttacks();
		}
	}

	public boolean isDefending() {
		return isFlipped() || Bit64.on(state, 1) || hasFlag(Flag.ALWAYS_DEFENSE);
	}

	public void setDefending(boolean defending) {
		state = Bit64.set(state, 1, defending);

		if (!isFlipped() && slot != null) {
			getGame().trigger(ON_SWITCH, asSource(ON_SWITCH));
		}
	}

	@Override
	public boolean isFlipped() {
		return Bit64.on(state, 2);
	}

	@Override
	public void setFlipped(boolean flipped) {
		boolean trigger = isFlipped() && !flipped && slot != null;

		state = Bit64.set(state, 2, flipped);
		if (trigger) {
			setDefending(true);

			if (hand != null && getGame() != null) {
				if (getGame().getCurrentSide() != hand.getSide()) {
					getGame().trigger(ON_FLIP, asSource(ON_FLIP));
				} else {
					getGame().trigger(ON_SUMMON, asSource(ON_SUMMON));
				}
			}
		}
	}

	public boolean isSealed() {
		return Bit64.on(state, 3);
	}

	public void setSealed(boolean sealed) {
		state = Bit64.set(state, 3, sealed);
	}

	@Override
	public boolean isEthereal() {
		return Bit64.on(state, 4);
	}

	@Override
	public void setEthereal(boolean ethereal) {
		state = Bit64.set(state, 4, ethereal);
	}

	public boolean hasSwitched() {
		return Bit64.on(state, 5);
	}

	public void setSwitched(boolean switched) {
		state = Bit64.set(state, 5, switched);
	}

	@Override
	public boolean isManipulated() {
		return Bit64.on(state, 6);
	}

	@Override
	public void setManipulated(boolean manipulated) {
		state = Bit64.set(state, 6, manipulated);
	}

	public boolean isSleeping() {
		return !isStunned() && Bit64.on(state, 2, 4);
	}

	public int getRemainingSleep() {
		return (int) Bit64.get(state, 2, 4);
	}

	public void awaken() {
		long curr = Bit64.get(state, 2, 4);

		if (getGame().getArena().getField().getType() != FieldType.NIGHT && getGame().chance(100d / (curr + 1))) {
			state = Bit64.set(state, 2, 0, 4);
		}
	}

	public void setSleep(int time) {
		if (hasFlag(Flag.NO_SLEEP, true)) return;

		long curr = Bit64.get(state, 2, 4);
		state = Bit64.set(state, 2, Math.max(curr, time), 4);
	}

	public void reduceSleep(int time) {
		long curr = Bit64.get(state, 2, 4);
		state = Bit64.set(state, 2, Math.max(0, curr - time), 4);
	}

	public boolean isStunned() {
		return !isStasis() && Bit64.on(state, 3, 4);
	}

	public int getRemainingStun() {
		return (int) Bit64.get(state, 3, 4);
	}

	public void setStun(int time) {
		if (hasFlag(Flag.NO_STUN, true)) return;

		long curr = Bit64.get(state, 3, 4);
		state = Bit64.set(state, 3, Math.max(curr, time), 4);
	}

	public void reduceStun(int time) {
		long curr = Bit64.get(state, 3, 4);
		state = Bit64.set(state, 3, Math.max(0, curr - time), 4);
	}

	public boolean isStasis() {
		return Bit64.on(state, 4, 4);
	}

	public int getRemainingStasis() {
		return (int) Bit64.get(state, 4, 4);
	}

	public void setStasis(int time) {
		if (hasFlag(Flag.NO_STASIS, true)) return;

		long curr = Bit64.get(state, 4, 4);
		state = Bit64.set(state, 4, Math.max(curr, time), 4);
	}

	public void reduceStasis(int time) {
		long curr = Bit64.get(state, 4, 4);
		state = Bit64.set(state, 4, Math.max(0, curr - time), 4);
	}

	public Senshi getTarget() {
		return target;
	}

	public int getRemainingTaunt() {
		int taunt = (int) Bit64.get(state, 5, 4);
		if (taunt == 0 || (target == null || target.getSide() == getSide() || target.getIndex() == -1)) {
			state = Bit64.set(state, 5, 0, 4);
			target = null;
			taunt = 0;
		}

		return taunt;
	}

	public void setTaunt(Senshi target, int time) {
		if (target == null || hasFlag(Flag.NO_TAUNT, true)) return;

		this.target = target;
		long curr = Bit64.get(state, 5, 4);
		state = Bit64.set(state, 5, Math.max(curr, time), 4);
	}

	public void reduceTaunt(int time) {
		long curr = Bit64.get(state, 5, 4);
		state = Bit64.set(state, 5, Math.max(0, curr - time), 4);
	}

	public boolean isBerserk() {
		return Bit64.on(state, 6, 4);
	}

	public int getRemainingBerserk() {
		return (int) Bit64.get(state, 6, 4);
	}

	public void setBerserk(int time) {
		if (hasFlag(Flag.NO_BERSERK, true)) return;

		long curr = Bit64.get(state, 6, 4);
		state = Bit64.set(state, 6, Math.max(curr, time), 4);
	}

	public void reduceBerserk(int time) {
		long curr = Bit64.get(state, 6, 4);
		state = Bit64.set(state, 6, Math.max(0, curr - time), 4);
	}

	public void reduceDebuffs(int time) {
		reduceStun(time);
		reduceSleep(time);
		reduceTaunt(time);
		reduceBerserk(time);
	}

	public boolean hasStatusEffect() {
		return !isAvailable()
			   || isSleeping()
			   || isStunned()
			   || isStasis()
			   || getRemainingTaunt() > 0
			   || isBerserk()
			   || isManipulated();
	}

	@Override
	public int getCooldown() {
		return (int) Bit64.get(state, 7, 4);
	}

	@Override
	public void setCooldown(int time) {
		long curr = Bit64.get(state, 7, 4);
		state = Bit64.set(state, 7, Math.max(curr, time), 4);
	}

	public void reduceCooldown(int time) {
		long curr = Bit64.get(state, 7, 4);
		state = Bit64.set(state, 7, Math.max(0, curr - time), 4);
	}

	public int getAttacks() {
		return (int) Bit64.get(state, 8, 4);
	}

	public int getRemAttacks() {
		int base = 1 + (int) stats.getAttacks().get();
		for (Evogear e : equipments) {
			base += (int) e.getStats().getAttacks().get();
			if (e.hasCharm(Charm.BARRAGE)) {
				base += Charm.BARRAGE.getValue(e.getTier());
			}
		}

		return base - getAttacks();
	}

	public boolean canAttack() {
		return slot != null && isAvailable() && !isFlipped() && !hasFlag(Flag.NO_ATTACK, true);
	}

	public boolean spendAttack() {
		if (hasFlag(Flag.FREE_ACTION, true)) return true;

		long curr = Bit64.get(state, 8, 4);
		state = Bit64.set(state, 8, curr + 1, 4);
		return getRemAttacks() > 0;
	}

	public void resetAttacks() {
		state = Bit64.set(state, 8, 0, 4);
	}

	public Senshi getLastInteraction() {
		return lastInteraction;
	}

	public void setLastInteraction(Senshi last) {
		this.lastInteraction = last;
	}

	@Override
	public ListOrderedSet<String> getCurses() {
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
		return hasFlag(Flag.BLIND, pop);
	}

	public boolean isSupporting() {
		return slot != null && slot.hasBottom() && slot.getBottom().SERIAL == SERIAL;
	}

	public Senshi getFrontline() {
		if (slot == null || !isSupporting()) return null;

		return slot.getTop();
	}

	public void setFrontline(Senshi card) {
		if (slot == null || !isSupporting()) return;

		slot.setTop(card);
	}

	public Senshi getSupport() {
		if (slot == null || isSupporting()) return null;

		return slot.getBottom();
	}

	public void setSupport(Senshi card) {
		if (slot == null || isSupporting()) return;

		slot.setBottom(card);
	}

	public String getEffect() {
		EffectHolder<?> source = getSource();
		return Utils.getOr(source.getStats().getEffect(), source.getBase().getEffect());
	}

	@Override
	public boolean hasEffect() {
		return !isSealed() && !getEffect().isBlank() && !hasFlag(Flag.NO_EFFECT);
	}

	public boolean hasAbility() {
		if (getEffect().contains(ON_ACTIVATE.name())) {
			return true;
		}

		for (Evogear e : equipments) {
			if (e.getEffect().contains(ON_ACTIVATE.name())) {
				return true;
			}
		}

		return false;
	}

	public TargetType getTargetType() {
		return targetType;
	}

	public void setTargetType(TargetType type) {
		targetType = type;
	}

	@Override
	public Trigger getCurrentTrigger() {
		return currentTrigger;
	}

	@Override
	public CachedScriptManager getCSM() {
		return cachedEffect;
	}

	public boolean execute(EffectParameters ep) {
		if (!hasTrueEffect(true)) {
			if (hand.getLockTime(Lock.EFFECT) > 0) return false;
			else if (hasFlag(Flag.NO_EFFECT, true)) {
				base.lockAll();
				return false;
			}
		} else if (hand.getOther().getOrigins().synergy() == Race.NIGHTMARE && isSleeping()) {
			return false;
		}

		Trigger trigger = null;
		boolean targeted = false;

		if (ep.trigger().name().startsWith("ON_DEFER")) {
			trigger = ep.trigger();
		} else {
			if (equals(ep.source().card())) {
				trigger = ep.source().trigger();
			} else {
				for (Target target : ep.targets()) {
					if (equals(target.card())) {
						trigger = target.trigger();
						break;
					}
				}

				if (trigger == null) {
					trigger = ep.trigger();
				}

				targeted = true;
			}
		}

		if (trigger == ON_ACTIVATE && getCooldown() > 0) return false;

		Shoukan game = getGame();
		if (base.isLocked(trigger) || trigger == NONE) {
			return false;
		}

		try {
			base.lock(trigger);
			if (getSlot().getIndex() > -1 && trigger != ON_TICK) {
				execute(new EffectParameters(ON_TICK, getSide(), asSource(ON_TICK)));
			}

			currentTrigger = trigger;
			if (Utils.equalsAny(trigger, ON_EFFECT_TARGET, ON_DEFEND)) {
				if (!game.getCurrent().equals(hand)) {
					Set<String> triggered = new HashSet<>();

					for (SlotColumn sc : game.getSlots(getSide())) {
						for (Senshi card : sc.getCards()) {
							if (card instanceof TrapSpell && card.isFlipped() && !triggered.contains(card.getId()) && !ep.isTarget(card)) {
								EffectParameters params;
								if (targeted) {
									params = new EffectParameters(
											ON_TRAP, getSide(),
											card.asSource(ON_TRAP),
											asTarget(trigger, TargetType.ALLY),
											ep.source().toTarget(TargetType.ENEMY)
									);
								} else {
									params = new EffectParameters(
											ON_TRAP, getSide(),
											card.asSource(ON_TRAP),
											asTarget(trigger, TargetType.ALLY)
									);
								}

								if (game.activateTrap(card, params)) {
									triggered.add(card.getId());
									game.getChannel().sendMessage(game.getString("str/trap_activation", card)).queue();
								}
							}
						}
					}
				}
			}

			for (Evogear e : List.copyOf(equipments)) {
				e.execute(new EffectParameters(trigger, getSide(), ep.source(), ep.targets()));
			}

			if (hand.getOrigins().synergy() == Race.EX_MACHINA) {
				Senshi sup = getSupport();
				if (sup != null) {
					Evogear e = new EquippableSenshi(sup);
					e.setEquipper(this);
					e.setHand(hand);

					e.execute(new EffectParameters(trigger, getSide(), ep.source(), ep.targets()));
				}
			}

			if (hasEffect() && getEffect().contains(trigger.name())) {
				if (isStunned() && getGame().chance(25)) {
					if (Trigger.getAnnounceable().contains(trigger) && !ep.isDeferred(Trigger.getAnnounceable())) {
						game.getChannel().sendMessage(game.getString("str/effect_stunned", this)).queue();
					}
				} else {
					CachedScriptManager csm = getCSM();
					csm.assertOwner(getSource(), () -> parseDescription(hand, getGame().getLocale()))
							.forScript(getEffect())
							.withConst("me", this)
							.withConst("self", this)
							.withConst("game", getGame())
							.withConst("data", stats.getData())
							.withVar("ep", ep.forSide(getSide()))
							.withVar("side", getSide())
							.withVar("trigger", trigger);

					if (this instanceof PlaceableEvogear pe) {
						csm.withConst("evo", pe.getOriginal());
					}

					csm.run();

					if (trigger != ON_TICK) {
						hasFlag(Flag.EMPOWERED, true);
					}

					if (trigger == ON_ACTIVATE) {
						hand.getData().put("last_ability", this);
						getGame().trigger(ON_ABILITY, getSide());
					}
				}
			}

			if (ep.referee() == null && trigger != ON_TICK) {
				Senshi sup = getSupport();
				if (sup != null) {
					sup.execute(new EffectParameters(ON_DEFER_SUPPORT, getSide(), new DeferredTrigger(this, trigger), ep.source(), ep.targets()));
				}

				for (Senshi adj : getNearby()) {
					adj.execute(new EffectParameters(ON_DEFER_NEARBY, getSide(), new DeferredTrigger(this, trigger), ep.source(), ep.targets()));
				}
			}

			for (@Language("Groovy") String curse : stats.getCurses()) {
				if (curse.isBlank() || !curse.contains(trigger.name())) continue;

				Utils.exec(toString(), curse, Map.of(
						"self", this,
						"game", getGame(),
						"data", stats.getData(),
						"ep", ep,
						"side", hand.getSide(),
						"props", getCSM().getStoredProps(),
						"trigger", trigger
				));
			}

			return true;
		} catch (TargetException e) {
			if (targetType != TargetType.NONE && trigger == ON_ACTIVATE) {
				if (Arrays.stream(ep.targets()).allMatch(t -> t.skip().get())) {
					setAvailable(false);
					return false;
				}

				game.getChannel().sendMessage(game.getString("error/target", game.getString("str/target_" + targetType))).queue();
			}

			return false;
		} catch (ActivationException e) {
			if (e instanceof SelectionException && trigger != ON_ACTIVATE) return false;

			game.getChannel().sendMessage(game.getString("error/activation", game.getString(e.getMessage()))).queue();
			return false;
		} catch (Exception e) {
			Drawable<?> source = Utils.getOr(stats.getSource(), this);

			game.getChannel().sendMessage(game.getString("error/effect")).queue();
			Constants.LOGGER.warn("Failed to execute {} effect\n{}", this, "/* " + source + " */\n" + getEffect(), e);
			return false;
		} finally {
			currentTrigger = null;
			base.unlock(trigger);
		}
	}

	@Override
	public void executeAssert(Trigger trigger) {
		if (!Utils.equalsAny(trigger, ON_INITIALIZE, ON_REMOVE)) return;
		else if (!getEffect().contains(trigger.name())) return;

		if (trigger == ON_INITIALIZE) {
			if (getBase().getTags().contains("AUGMENT") && !(this instanceof AugmentSenshi)) {
				replace(new AugmentSenshi(this, Senshi.getRandom(getGame().getRng())));
				return;
			}
		} else if (trigger == ON_REMOVE) {
			getGame().unbind(this);
		}

		try {
			CachedScriptManager csm = getCSM();
			csm.assertOwner(getSource(), () -> parseDescription(hand, getGame().getLocale()))
					.forScript(getEffect())
					.withConst("me", this)
					.withConst("self", this)
					.withConst("game", getGame())
					.withConst("data", stats.getData())
					.withVar("ep", new EffectParameters(trigger, getSide()))
					.withVar("side", getSide())
					.withVar("trigger", trigger);

			if (this instanceof PlaceableEvogear pe) {
				csm.withConst("evo", pe.getOriginal());
			}

			csm.run();
		} catch (Exception e) {
			Drawable<?> source = Utils.getOr(stats.getSource(), this);
			Constants.LOGGER.warn("Failed to initialize {}\n{}", this, "/* " + source + " */\n" + getEffect(), e);
		}
	}

	public void noEffect(Consumer<Senshi> c) {
		Set<Trigger> complement = EnumSet.complementOf(base.getLocks());

		try {
			base.lock(complement);
			c.accept(this);
		} finally {
			base.unlock(complement);
		}
	}

	public int getDamage(Senshi target) {
		if (target.isSupporting()) return 0;

		return getActiveAttr() - target.getActiveAttr();
	}

	public boolean isProtected(Drawable<?> source) {
		if (source instanceof EffectHolder<?> eh && eh.hasTrueEffect(true)) return false;
		else if (blocked.contains(source)) return true;

		if (hand != null) {
			if (hand.equals(source.getHand())) {
				return false;
			}

			if (getGame() != null) {
				getGame().trigger(ON_EFFECT_TARGET, source.asSource(), asTarget(ON_EFFECT_TARGET));
				if (isStasis() || hasFlag(Flag.IGNORE_EFFECT, true)) {
					return true;
				}
			}
		}

		if (source instanceof EffectHolder<?> eh) {
			boolean announce = Trigger.getAnnounceable().contains(eh.getCurrentTrigger());

			if (getGame().chance(getDodge())) {
				if (announce) {
					Shoukan game = getGame();
					game.getChannel().sendMessage(game.getLocale().get("str/avoid_effect", this)).queue();
					game.trigger(ON_DODGE, asSource(ON_DODGE));
				}

				return true;
			} else if (hasCharm(Charm.SHIELD, announce)) {
				blocked.add(source);
				if (announce) {
					Shoukan game = getGame();
					game.getChannel().sendMessage(game.getString("str/spell_shield", this)).queue();
					game.trigger(ON_PARRY, asSource(ON_PARRY));
				}

				return true;
			}
		}

		return false;
	}

	public void clearBlocked() {
		blocked.clear();
	}

	public ElementType getElement() {
		if (element == null) {
			ElementType[] elems = ElementType.values();
			element = elems[Calc.rng(0, elems.length - 1, id.hashCode())];
		}

		return element;
	}

	@Override
	public StashedCard getStashRef() {
		return stashRef;
	}

	@Override
	public void setStashRef(StashedCard sc) {
		stashRef = sc;
	}

	@Override
	public BondedList<?> getCurrentStack() {
		return currentStack;
	}

	@Override
	public void setCurrentStack(BondedList<?> stack) {
		currentStack = stack;
	}

	@Override
	public void reset() {
		equipments.clear();
		stats.clear();
		base.unlockAll();
		slot = null;
		lastInteraction = null;
		state = (state & 0b1000) | 0b1;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		if (hand == null) {
			hand = new Hand(deck);
		}

		BufferedImage out = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		DeckStyling style = deck.getStyling();
		Graph.applyTransformed(g2d, 15, 15, g1 -> {
			if (isFlipped()) {
				g1.drawImage(style.getFrame().getBack(deck), 0, 0, null);
				parseDescription(hand, getGame().getLocale());

				if (!isAvailable() || isManipulated()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}
			} else {
				Senshi card = Utils.getOr(stats.getDisguise(), this);
				String desc = isSealed() ? "" : card.getDescription(locale);
				BufferedImage img = card.getVanity().drawCardNoBorder(Utils.getOr(() -> stashRef.isChrome(), false));

				g1.setClip(style.getFrame().getBoundary());
				g1.drawImage(img, 0, 0, null);
				g1.setClip(null);

				g1.drawImage(style.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
				g1.drawImage(card.getRace().getIcon(), 190, 12, null);

				g1.setFont(FONT);
				g1.setColor(style.getFrame().getPrimaryColor());
				String name = Graph.abbreviate(g1, card.getVanity().getName(), MAX_NAME_WIDTH);

				Graph.drawOutlinedString(g1, name, 12, 30, 2, style.getFrame().getBackgroundColor());

				if (!stats.getWrite().isBlank()) {
					g1.setColor(Color.ORANGE);
					g1.setFont(Fonts.NOTO_SANS_EXTRABOLD.deriveBold(15f));

					String str = stats.getWrite();
					FontMetrics fm = g1.getFontMetrics();
					Graph.drawOutlinedString(g1, str,
							225 / 2 - fm.stringWidth(str) / 2, 39 + fm.getHeight() / 2,
							2, Color.BLACK
					);
				}

				if (!desc.isEmpty()) {
					drawDescription(g1, hand, locale);
				}

				if (!hasFlag(Flag.HIDE_STATS)) {
					card.drawCosts(g1);
					if (!isSupporting()) {
						card.drawAttributes(g1, !desc.isEmpty());
					}
				}

				if (!isAvailable() || isManipulated()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}

				if (hand != null && getGame() != null) {
					boolean legacy = hand.getUserDeck().getStyling().getFrame().isLegacy();
					String path = "shoukan/frames/state/" + (legacy ? "old" : "new");

					double mult = getFieldMult();
					if (mult != 1) {
						BufferedImage indicator = null;
						if (mult > 1) {
							indicator = IO.getResourceAsImage(path + "/buffed.png");
						} else if (mult < 1) {
							indicator = IO.getResourceAsImage(path + "/nerfed.png");
						}

						g2d.drawImage(indicator, 0, 0, null);
					}

					if (hasFlag(Flag.EMPOWERED)) {
						BufferedImage ovr = IO.getResourceAsImage(path + "/empowered.png");
						g2d.drawImage(ovr, 0, 0, null);
					}

					if (isEthereal()) {
						BufferedImage ovr = IO.getResourceAsImage(path + "/ethereal.png");
						g2d.drawImage(ovr, 0, 0, null);
					}

					if (isManipulated()) {
						BufferedImage ovr = IO.getResourceAsImage("shoukan/states/locked.png");
						g2d.drawImage(ovr, 15, 15, null);
					}
				}
			}

			boolean over = false;
			String[] states = {"stasis", "stun", "sleep", "berserk", "taunt", "defense"};
			int[] timers = {
					getRemainingStasis(),
					getRemainingStun(),
					getRemainingSleep(),
					getRemainingBerserk(),
					getRemainingTaunt()
			};
			for (int i = 0; i < timers.length; i++) {
				int time = timers[i];

				if (time > 0) {
					over = true;
					BufferedImage overlay = IO.getResourceAsImage("shoukan/states/" + states[i] + ".png");
					g1.drawImage(overlay, 0, 0, null);

					String str = locale.get("str/turns", time);

					g1.setColor(Graph.getColor(overlay).brighter());
					g1.setFont(Drawable.FONT.deriveFont(Drawable.FONT.getSize() * 2f));

					FontMetrics fm = g1.getFontMetrics();
					Graph.drawOutlinedString(g1, str,
							225 / 2 - fm.stringWidth(str) / 2, 9 + (225 / 2 + fm.getHeight() / 2),
							6, Color.BLACK
					);
					break;
				}
			}

			if (!over && !isFlipped() && isDefending()) {
				g1.drawImage(IO.getResourceAsImage("shoukan/states/defense.png"), 0, 0, null);
			}
		});

		g2d.dispose();

		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Senshi senshi = (Senshi) o;
		return SERIAL == senshi.SERIAL
			   && Objects.equals(id, senshi.id)
			   && Objects.equals(card, senshi.card);
	}

	public int posHash() {
		return Objects.hash(id, slot, isSupporting());
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, id, card);
	}

	@Override
	public Senshi fork() throws CloneNotSupportedException {
		Senshi clone = new Senshi(id, card, race, base.clone(), stats.clone(), stashRef);
		clone.stats = stats.clone();
		clone.hand = hand;
		clone.state = state & (0b1_1111 | 0xF_FFFFF_00);
		clone.stashRef = stashRef;

		return clone;
	}

	@Override
	public String toString() {
		if (isFlipped() && hand != null) {
			return getGame().getString("str/a_card");
		}

		return getVanity().getName();
	}

	public static Senshi getRandom(RandomGenerator rng) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT card_id FROM senshi WHERE NOT has(tags, 'FUSION') ORDER BY card_id");
		if (ids.isEmpty()) return null;

		return DAO.find(Senshi.class, Utils.getRandomEntry(rng, ids));
	}

	public static Senshi getRandom(RandomGenerator rng, String... filters) {
		XStringBuilder query = new XStringBuilder("""
				WITH senshi_truecost AS (
					SELECT card_id, atk, dfs, dodge, parry
						 , mana + iif(has(tags, 'FUSION'), 5, 0) as mana
						 , blood, sacrifices
						 , effect, tags, race
					FROM senshi
				)
				SELECT card_id
				FROM senshi_truecost
				""");
		for (String f : filters) {
			query.appendNewLine(f);
		}

		query.appendNewLine(" ORDER BY card_id");

		List<String> ids = DAO.queryAllNative(String.class, query.toString());
		if (ids.isEmpty()) return null;

		return DAO.find(Senshi.class, Utils.getRandomEntry(rng, ids));
	}

	public static XList<Senshi> getByTag(RandomGenerator rng, String... tags) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT by_tag('senshi', ?1)", (Object) tags);

		return new XList<>(DAO.queryAll(Senshi.class, "SELECT s FROM Senshi s WHERE s.id IN ?1 ORDER BY s.id", ids), rng);
	}
}
