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
import com.kuuhaku.model.records.shoukan.DeferredTrigger;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.*;
import jakarta.persistence.*;
import org.apache.commons.collections4.set.ListOrderedSet;
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
import java.util.random.RandomGenerator;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;

@Entity
@Table(name = "senshi")
public class Senshi extends DAO<Senshi> implements EffectHolder<Senshi> {
	@Transient
	public final String KLASS = getClass().getName();
	public transient long SERIAL = ThreadLocalRandom.current().nextLong();

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
	private transient BondedList<Evogear> equipments = new BondedList<>((e, it) -> {
		if (getEquipments().contains(e)) return false;

		e.setEquipper(this);
		e.setHand(this.getHand());
		e.executeAssert(ON_INITIALIZE);

		Shoukan game = this.getGame();
		game.trigger(ON_EQUIP, asSource(ON_EQUIP));

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
			game.putAtOpenSlot(this.getSide(), true, withCopy(s -> {
				s.getStats().getAttrMult().set(-1 + (0.25 * e.getTier()));
				s.getStats().getData().put("cloned", true);
			}));
		}

		return true;
	}, e -> {
		e.executeAssert(ON_REMOVE);
		e.setEquipper(null);
	});
	private transient CardExtra stats = new CardExtra();
	private transient SlotColumn slot = null;
	private transient Hand hand = null;
	private transient Hand leech = null;
	private transient Senshi target = null;
	private transient Senshi lastInteraction = null;
	private transient CachedScriptManager cachedEffect = new CachedScriptManager();
	private transient Set<Drawable<?>> blocked = new HashSet<>();
	private transient TargetType targetType = TargetType.NONE;
	private transient ElementType element = null;

	@Transient
	private int state = 0b10;
	/*
	0xF FFFFF FF
	  │ │││││ └┴ 0111 1111
	  │ │││││     │││ │││└ solid
	  │ │││││     │││ ││└─ available
	  │ │││││     │││ │└── defending
	  │ │││││     │││ └─── flipped
	  │ │││││     ││└ sealed
	  │ │││││     │└─ switched
	  │ │││││     └── ethereal
	  │ ││││└─ (0 - 15) sleeping
	  │ │││└── (0 - 15) stunned
	  │ ││└─── (0 - 15) stasis
	  │ │└──── (0 - 15) taunt
	  │ └───── (0 - 15) berserk
	  └ (0 - 15) cooldown
	 */

	public Senshi() {
	}

	public Senshi(String id, Card card, Race race, CardAttributes base) {
		this.id = id;
		this.card = card;
		this.race = race;
		this.base = base;
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
	public boolean hasFlag(Flag flag, boolean pop) {
		for (Evogear e : equipments) {
			if (e.hasFlag(flag, pop)) return true;
		}

		if (pop) return stats.getFlags().pop(flag);
		else return stats.getFlags().has(flag);
	}

	@Override
	public List<String> getTags() {
		List<String> out = new ArrayList<>();
		if (race != Race.NONE) {
			out.add("race/" + getRace().name());
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

		out.add("element/" + getElement().name().toLowerCase());

		List<String> tags = base.getTags().stream()
				.map(String::valueOf)
				.sorted()
				.toList();

		for (String tag : tags) {
			if (out.contains("tag/base") && tag.equals("MATERIAL")) continue;

			out.add("tag/" + tag.toLowerCase());
		}

		return out;
	}

	public BondedList<Evogear> getEquipments() {
		equipments.removeIf(e -> !equals(e.getEquipper()));

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
	public Hand getLeech() {
		return leech;
	}

	@Override
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
		EffectHolder<?> source = getSource();
		return Utils.getOr(source.getStats().getDescription(locale), source.getBase().getDescription(locale));
	}

	@Override
	public int getMPCost() {
		if (hand != null && hand.getOrigins().synergy() == Race.CELESTIAL) {
			return hand.getUserDeck().getAverageMPCost();
		}

		return Math.max(0, Calc.round((base.getMana() + stats.getMana().get() + (isFusion() ? 5 : 0)) * getCostMult()));
	}

	@Override
	public int getHPCost() {
		return Math.max(0, Calc.round((base.getBlood() + stats.getBlood().get()) * getCostMult()));
	}

	@Override
	public int getSCCost() {
		return Math.max(0, Calc.round((base.getSacrifices() + stats.getSacrifices().get()) * getCostMult()));
	}

	@Override
	public int getDmg() {
		int sum = base.getAtk() + (int) stats.getAtk().get() + getEquipDmg();

		double mult = 1;
		if (hand != null) {
			switch (hand.getOrigins().synergy()) {
				case ONI -> {
					if (hand.isLowLife()) {
						mult *= 1.2;
					}
				}
				case GHOUL -> {
					if (hand.getHPPrcnt() > 1) {
						mult *= 1 + Math.max(0, (hand.getHPPrcnt() - 1) / 2);
					}
				}
				case CYBERBEAST -> sum += getGame().getCards(getSide()).stream().mapToInt(Senshi::getBlock).sum();
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

		double mult = 1;
		if (hand != null) {
			mult *= getFieldMult();
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
		for (Map.Entry<String, Object> e : f.getModifiers().entrySet()) {
			Race r = Race.valueOf(e.getKey());

			double mod = 0;
			if (race.isRace(r)) {
				mod = ((Number) e.getValue()).doubleValue();

				if (race != r) {
					mod /= 2;
				}
			}

			if (mod != 0 && hand.getOrigins().synergy() == Race.ELF) {
				mod += 0.1;
			}

			if (mod != 0 && hand.getOther().getOrigins().synergy() == Race.DARK_ELF) {
				mod -= 0.15;
			}

			mult += mod;
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

		int sum = base.getDodge() + (int) stats.getDodge().get() + getEquipDodge();

		int min = 0;
		if (hand != null && hand.getOrigins().synergy() == Race.GEIST) {
			min += 10;
		}

		if (isBlinded()) {
			sum /= 2;
		}

		if (hand != null && getGame() != null && getGame().getArena().getField().getType() == FieldType.DUNGEON) {
			return Utils.clamp(sum, min, 50);
		}

		return Utils.clamp(min + sum, min, 100);
	}

	@Override
	public int getBlock() {
		int sum = base.getBlock() + (int) stats.getBlock().get() + getEquipBlock();

		int min = 0;
		if (hand != null) {
			if (hand.getOrigins().synergy() == Race.WEREBEAST && isSleeping()) {
				sum += 50;
			} else if (hand.getOrigins().synergy() == Race.CYBORG) {
				min += 10;
			}
		}

		return Utils.clamp(min + sum, min, 100);
	}

	@Override
	public double getCostMult() {
		double mult = stats.getCostMult().get();
		if (hand != null) {
			if (hand.getOrigins().isPure() && race == hand.getOrigins().major()) {
				mult *= 0.66;
			}

			if (hand.getOrigins().synergy() == Race.PIXIE) {
				mult *= getFieldMult();
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
			}
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
				mult *= 1 + Calc.prcnt(hand.getUserDeck().getEvoWeight(), 24) / 2;
			}
		}

		return mult;
	}

	public int getEquipDmg() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().filter(Evogear::isAvailable).mapToInt(Evogear::getDmg).sum();
	}

	public int getEquipDfs() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream()
				.filter(Evogear::isAvailable)
				.mapToInt(Evogear::getDfs).sum();
	}

	public int getEquipDodge() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream()
				.filter(Evogear::isAvailable)
				.mapToInt(Evogear::getDodge).sum();
	}

	public int getEquipBlock() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream()
				.filter(Evogear::isAvailable)
				.mapToInt(Evogear::getBlock).sum();
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
	public boolean isSolid() {
		return !isEthereal() && Bit.on(state, 0) && !base.getTags().contains("FUSION");
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
		return isFlipped() || Bit.on(state, 2) || hasFlag(Flag.ALWAYS_DEFENSE);
	}

	public void setDefending(boolean defending) {
		state = Bit.set(state, 2, defending);

		if (!isFlipped() && slot != null) {
			getGame().trigger(ON_SWITCH, asSource(ON_SWITCH));
		}
	}

	public boolean canAttack() {
		return slot != null && isAvailable() && !isFlipped() && !hasFlag(Flag.NO_ATTACK, true);
	}

	@Override
	public boolean isFlipped() {
		return Bit.on(state, 3);
	}

	@Override
	public void setFlipped(boolean flipped) {
		boolean trigger = isFlipped() && !flipped && slot != null;

		state = Bit.set(state, 3, flipped);
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
		return Bit.on(state, 4);
	}

	public void setSealed(boolean sealed) {
		state = Bit.set(state, 4, sealed);
	}

	public boolean hasSwitched() {
		return Bit.on(state, 5);
	}

	public void setSwitched(boolean switched) {
		state = Bit.set(state, 5, switched);
	}

	@Override
	public boolean isEthereal() {
		return Bit.on(state, 6);
	}

	@Override
	public void setEthereal(boolean ethereal) {
		state = Bit.set(state, 6, ethereal);
	}

	public boolean isSleeping() {
		return !isStunned() && Bit.on(state, 2, 4);
	}

	public int getRemainingSleep() {
		return Bit.get(state, 2, 4);
	}

	public void awaken() {
		int curr = Bit.get(state, 2, 4);

		if (getGame().getArena().getField().getType() != FieldType.NIGHT && getGame().chance(100d / (curr + 1))) {
			state = Bit.set(state, 2, 0, 4);
		}
	}

	public void setSleep(int time) {
		if (hasFlag(Flag.NO_SLEEP, true)) return;

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

	public int getRemainingStun() {
		return Bit.get(state, 3, 4);
	}

	public void setStun(int time) {
		if (hasFlag(Flag.NO_STUN, true)) return;

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

	public int getRemainingStasis() {
		return Bit.get(state, 4, 4);
	}

	public void setStasis(int time) {
		if (hasFlag(Flag.NO_STASIS, true)) return;

		int curr = Bit.get(state, 4, 4);
		state = Bit.set(state, 4, Math.max(curr, time), 4);
	}

	public void reduceStasis(int time) {
		int curr = Bit.get(state, 4, 4);
		state = Bit.set(state, 4, Math.max(0, curr - time), 4);
	}

	public Senshi getTarget() {
		return target;
	}

	public int getRemainingTaunt() {
		int taunt = Bit.get(state, 5, 4);
		if (taunt == 0 || (target == null || target.getSide() == getSide() || target.getIndex() == -1)) {
			state = Bit.set(state, 5, 0, 4);
			target = null;
			taunt = 0;
		}

		return taunt;
	}

	public void setTaunt(Senshi target, int time) {
		if (target == null || hasFlag(Flag.NO_TAUNT, true)) return;

		this.target = target;
		int curr = Bit.get(state, 5, 4);
		state = Bit.set(state, 5, Math.max(curr, time), 4);
	}

	public void reduceTaunt(int time) {
		int curr = Bit.get(state, 5, 4);
		state = Bit.set(state, 5, Math.max(0, curr - time), 4);
	}

	public boolean isBerserk() {
		return Bit.on(state, 6, 4);
	}

	public int getRemainingBerserk() {
		return Bit.get(state, 6, 4);
	}

	public void setBerserk(int time) {
		if (hasFlag(Flag.NO_BERSERK, true)) return;

		int curr = Bit.get(state, 6, 4);
		state = Bit.set(state, 6, Math.max(curr, time), 4);
	}

	public void reduceBerserk(int time) {
		int curr = Bit.get(state, 6, 4);
		state = Bit.set(state, 6, Math.max(0, curr - time), 4);
	}

	public void reduceDebuffs(int time) {
		reduceStun(time);
		reduceSleep(time);
		reduceTaunt(time);
		reduceBerserk(time);
	}

	@Override
	public int getCooldown() {
		return Bit.get(state, 7, 4);
	}

	@Override
	public void setCooldown(int time) {
		int curr = Bit.get(state, 7, 4);
		state = Bit.set(state, 7, Math.max(curr, time), 4);
	}

	public void reduceCooldown(int time) {
		int curr = Bit.get(state, 7, 4);
		state = Bit.set(state, 7, Math.max(0, curr - time), 4);
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
	public CachedScriptManager getCSM() {
		return cachedEffect;
	}

	@Override
	public boolean execute(EffectParameters ep) {
		return execute(false, ep);
	}

	public boolean execute(boolean global, EffectParameters ep) {
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
			if (Utils.equalsAny(trigger, ON_EFFECT_TARGET, ON_DEFEND)) {
				if (!game.getCurrent().equals(hand)) {
					Set<String> triggered = new HashSet<>();

					for (SlotColumn sc : game.getSlots(getSide())) {
						for (Senshi card : sc.getCards()) {
							if (card instanceof TrapSpell p && !triggered.contains(p.getId())) {
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

								if (game.activateProxy(card, params)) {
									triggered.add(p.getId());
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

			if (hasEffect() && getEffect().contains(trigger.name())) {
				if (isStunned() && getGame().chance(25)) {
					if (!global) {
						game.getChannel().sendMessage(game.getString("str/effect_stunned", this)).queue();
					}
				} else {
					CachedScriptManager csm = getCSM();
					csm.assertOwner(getSource(), () -> parseDescription(getGame().getLocale()))
							.forScript(getEffect())
							.withConst("me", this)
							.withConst("self", this)
							.withConst("game", getGame())
							.withConst("data", stats.getData())
							.withVar("ep", ep.forSide(getSide()))
							.withVar("side", getSide())
							.withVar("trigger", trigger)
							.run();

					if (trigger != ON_TICK) {
						hasFlag(Flag.EMPOWERED, true);
					}
				}
			}

			if (ep.referee() == null) {
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
			Constants.LOGGER.warn("Failed to execute " + this + " effect\n" + ("/* " + source + " */\n" + getEffect()), e);
			return false;
		} finally {
			base.unlock(trigger);
		}
	}

	@Override
	public void executeAssert(Trigger trigger) {
		if (!Utils.equalsAny(trigger, ON_INITIALIZE, ON_REMOVE)) return;
		else if (!hasEffect() || !getEffect().contains(trigger.name())) return;

		if (trigger == ON_INITIALIZE) {
			if (getBase().getTags().contains("AUGMENT") && !(this instanceof AugmentSenshi)) {
				replace(new AugmentSenshi(this, Senshi.getRandom(getGame().getRng())));
				return;
			}
		}

		try {
			CachedScriptManager csm = getCSM();
			csm.assertOwner(getSource(), () -> parseDescription(getGame().getLocale()))
					.forScript(getEffect())
					.withConst("me", this)
					.withConst("self", this)
					.withConst("game", getGame())
					.withConst("data", stats.getData())
					.withVar("ep", new EffectParameters(trigger, getSide()))
					.withVar("side", getSide())
					.withVar("trigger", trigger)
					.run();
		} catch (Exception e) {
			Drawable<?> source = Utils.getOr(stats.getSource(), this);
			Constants.LOGGER.warn("Failed to initialize " + this + "\n" + ("/* " + source + " */\n" + getEffect()), e);
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

		if (getGame().chance(getDodge())) {
			Shoukan game = getGame();
			game.getChannel().sendMessage(game.getLocale().get("str/avoid_effect",
					this.isFlipped() ? game.getLocale().get("str/a_card") : this
			)).queue();
			return true;
		} else if (hasCharm(Charm.SHIELD, true)) {
			blocked.add(source);
			Shoukan game = getGame();
			game.getChannel().sendMessage(game.getString("str/spell_shield", this)).queue();
			return true;
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
	public boolean keepOnDestroy() {
		return !isFusion();
	}

	@Override
	public void reset() {
		equipments.clear();
		stats.clear();
		base.unlockAll();
		slot = null;
		leech = null;
		lastInteraction = null;

		byte base = 0b10;
		base = (byte) Bit.set(base, 4, isSealed());

		state = base;
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
				parseDescription(getGame().getLocale());
			} else {
				Senshi card = Utils.getOr(stats.getDisguise(), this);
				String desc = isSealed() ? "" : card.getDescription(locale);
				BufferedImage img = card.getVanity().drawCardNoBorder(style.isUsingChrome());

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
					drawDescription(g1, locale);
				}

				if (!hasFlag(Flag.HIDE_STATS)) {
					card.drawCosts(g1);
					if (!isSupporting()) {
						card.drawAttributes(g1, !desc.isEmpty());
					}
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
						BufferedImage emp = IO.getResourceAsImage(path + "/empowered.png");
						g2d.drawImage(emp, 0, 0, null);
					}

					if (isEthereal()) {
						BufferedImage emp = IO.getResourceAsImage(path + "/ethereal.png");
						g2d.drawImage(emp, 0, 0, null);
					}
				}
			}

			if (!isAvailable()) {
				RescaleOp op = new RescaleOp(0.5f, 0, null);
				op.filter(out, out);
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
		return Objects.equals(id, senshi.id) && Objects.equals(card, senshi.card) && SERIAL == senshi.SERIAL;
	}

	public int posHash() {
		return Objects.hash(id, slot, isSupporting());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card, SERIAL);
	}

	@Override
	public Senshi fork() throws CloneNotSupportedException {
		Senshi clone = new Senshi(id, card, race, base.clone());
		clone.stats = stats.clone();
		clone.hand = hand;
		clone.state = state & 0b11110;

		return clone;
	}

	@Override
	public String toString() {
		return getVanity().getName();
	}

	public static Senshi getRandom(RandomGenerator rng) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT card_id FROM senshi WHERE NOT has(tags, 'FUSION') ORDER BY card_id");
		if (ids.isEmpty()) return null;

		return DAO.find(Senshi.class, Utils.getRandomEntry(rng, ids));
	}

	public static Senshi getRandom(RandomGenerator rng, String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM senshi");
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
