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
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.exceptions.SelectionException;
import com.kuuhaku.exceptions.TargetException;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.CachedScriptManager;
import com.kuuhaku.model.common.XList;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.CardProxy;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.model.records.shoukan.Source;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.*;
import com.kuuhaku.util.json.JSONObject;
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

	@OneToOne(optional = false, orphanRemoval = true)
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
		e.setEquipper(this);
		e.setHand(getHand());
		e.executeAssert(Trigger.ON_INITIALIZE);

		Shoukan game = getHand().getGame();
		game.trigger(Trigger.ON_EQUIP, asSource(Trigger.ON_EQUIP));

		if (e.hasCharm(Charm.TIMEWARP)) {
			int times = Charm.TIMEWARP.getValue(e.getTier());
			for (int i = 0; i < times; i++) {
				game.trigger(Trigger.ON_TURN_BEGIN, asSource(Trigger.ON_TURN_BEGIN));
				game.trigger(Trigger.ON_TURN_END, asSource(Trigger.ON_TURN_END));
			}
		}

		if (e.hasCharm(Charm.CLONE)) {
			game.putAtOpenSlot(getSide(), true,
					withCopy(s -> s.getStats().setAttrMult(-1 + (0.25 * e.getTier())))
			);
		}

		return true;
	}, e -> {
		e.executeAssert(Trigger.ON_REMOVE);
		e.setEquipper(null);
	});
	private transient CardExtra stats = new CardExtra();
	private transient SlotColumn slot = null;
	private transient Hand hand = null;
	private transient Hand leech = null;
	private transient Senshi target = null;
	private transient Senshi lastInteraction = null;
	private transient CachedScriptManager cachedEffect = new CachedScriptManager();

	@Transient
	private int state = 0b10;
	/*
	0x0F FFFF FF
	   │ ││││ └┴ 0011 1111
	   │ ││││      ││ │││└ solid
	   │ ││││      ││ ││└─ available
	   │ ││││      ││ │└── defending
	   │ ││││      ││ └─── flipped
	   │ ││││      │└ sealed
	   │ ││││      └─ switched
	   │ │││└─ (0 - 15) sleeping
	   │ ││└── (0 - 15) stunned
	   │ │└─── (0 - 15) stasis
	   │ └──── (0 - 15) taunt
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

	public boolean hasFlag(Flag flag) {
		for (Evogear e : equipments) {
			if (e.getStats().hasFlag(flag)) return true;
		}

		return stats.hasFlag(flag);
	}

	public boolean popFlag(Flag flag) {
		for (Evogear e : equipments) {
			if (e.getStats().popFlag(flag)) return true;
		}

		return stats.popFlag(flag);
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

		for (Object tag : base.getTags()) {
			if (out.contains("tag/base") && tag.equals("MATERIAL")) continue;

			out.add("tag/" + ((String) tag).toLowerCase());
		}

		return out;
	}

	public BondedList<Evogear> getEquipments() {
		equipments.removeIf(e -> !equals(e.getEquipper()) || isSupporting());

		while (equipments.size() > 3) {
			hand.getGraveyard().add(equipments.removeFirst());
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

	public boolean hasCharm(Charm charm) {
		return equipments.stream().anyMatch(e -> e.hasCharm(charm));
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
		return Utils.getOr(slot, new SlotColumn(hand.getGame(), hand.getSide(), -1));
	}

	@Override
	public void setSlot(SlotColumn slot) {
		this.slot = slot;
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
		EffectHolder<?> source = (EffectHolder<?>) Utils.getOr(stats.getSource(), this);

		return Utils.getOr(source.getStats().getDescription(locale), source.getBase().getDescription(locale));
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
			if (hand.getOrigin().hasMinor(Race.UNDEAD)) {
				mult *= 1 + (hand.getGraveyard().size() * 0.005);
			}

			if (hand.isLowLife() && hand.getOrigin().synergy() == Race.ONI) {
				mult *= 1.02;
			} else if (hand.getHPPrcnt() > 1 && hand.getOrigin().synergy() == Race.GHOUL) {
				mult *= 1.05;
			}

			mult *= getFieldMult();
		}

		if (isStunned()) {
			mult /= 2;
		}

		return (int) Math.max(0, sum * mult * getAttrMult());
	}

	@Override
	public int getDfs() {
		int sum = base.getDfs() + stats.getDfs() + getEquipDfs();

		double mult = 1;
		if (hand != null) {
			if (hand.getOrigin().hasMinor(Race.SPIRIT)) {
				mult *= 1 + (hand.getGraveyard().size() * 0.01);
			}

			mult *= getFieldMult();
		}

		if (isStunned()) {
			mult /= 2;
		}

		return (int) Math.max(0, sum * mult * getAttrMult());
	}

	public double getFieldMult() {
		if (hasFlag(Flag.IGNORE_FIELD)) return 1;
		Field f = hand.getGame().getArena().getField();

		double mult = 1;
		Race[] races = getRace().split();
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
		int sum = (int) ((base.getDodge() + stats.getDodge() + getEquipDodge()) * getAttrMult());
		if (hand != null && hand.getGame().getArena().getField().getType() == FieldType.DUNGEON) {
			sum = Math.min(sum, 50);
		}

		return Utils.clamp(sum, 0, 100);
	}

	@Override
	public int getBlock() {
		int sum = base.getBlock() + stats.getBlock() + getEquipBlock();

		int min = 0;
		if (hand != null && hand.getOrigin().synergy() == Race.CYBORG) {
			min += 2;
		}

		return (int) Utils.clamp((min + sum) * getAttrMult(), 0, 100);
	}

	private double getAttrMult() {
		double mult = 1;
		if (hand != null) {
			if (hand.getOrigin().isPure()) {
				if (race == hand.getOrigin().major()) {
					mult *= 1.25;
				} else {
					mult *= 0.5;
				}
			}

			mult *= 1 - Math.max(0, 0.06 * (hand.getOrigin().minor().length - 1));
		}

		return mult * stats.getAttrMult();
	}

	public double getPower() {
		if (isSupporting()) {
			return stats.getPower() * 0.75;
		} else {
			return stats.getPower();
		}
	}

	public int getEquipDmg() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getDmg).sum();
	}

	public int getEquipDfs() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getDfs).sum();
	}

	public int getEquipDodge() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getDodge).sum();
	}

	public int getEquipBlock() {
		if (hasFlag(Flag.NO_EQUIP)) return 0;

		return equipments.stream().mapToInt(Evogear::getBlock).sum();
	}

	public int getActiveEquips() {
		try {
			if (isDefending()) return getEquipDfs();
			return getEquipDmg();
		} finally {
			popFlag(Flag.NO_EQUIP);
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
			popFlag(Flag.NO_EQUIP);
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

		if (!isFlipped() && slot != null) {
			hand.getGame().trigger(Trigger.ON_SWITCH, asSource(Trigger.ON_SWITCH));
		}
	}

	public boolean canAttack() {
		return (!isDefending() || (popFlag(Flag.ALWAYS_ATTACK) && !isFlipped())) && isAvailable();
	}

	@Override
	public boolean isFlipped() {
		return Bit.on(state, 3);
	}

	@Override
	public void setFlipped(boolean flipped) {
		if (isFlipped() && !flipped && slot != null) {
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

	public boolean isSealed() {
		return Bit.on(state, 4);
	}

	public void setSealed(boolean sealed) {
		state = Bit.set(state, 4, sealed);
	}

	public boolean hasSwitched() {
		return Bit.on(state, 5);
	}

	public void setSwitched(boolean sealed) {
		state = Bit.set(state, 5, sealed);
	}

	public boolean isSleeping() {
		return !isStunned() && Bit.on(state, 3, 4);
	}

	public int getRemainingSleep() {
		return Bit.get(state, 3, 4);
	}

	public void awake() {
		int curr = Bit.get(state, 3, 4);

		if (Calc.chance(100d / (curr + 1))) {
			state = Bit.set(state, 3, 0, 4);
		}
	}

	public void setSleep(int time) {
		if (popFlag(Flag.NO_SLEEP)) return;

		int curr = Bit.get(state, 3, 4);
		state = Bit.set(state, 3, Math.max(curr, time), 4);
	}

	public void reduceSleep(int time) {
		int curr = Bit.get(state, 3, 4);
		state = Bit.set(state, 3, Math.max(0, curr - time), 4);
	}

	public boolean isStunned() {
		return !isStasis() && Bit.on(state, 4, 4);
	}

	public int getRemainingStun() {
		return Bit.get(state, 4, 4);
	}

	public void setStun(int time) {
		if (popFlag(Flag.NO_STUN)) return;

		int curr = Bit.get(state, 4, 4);
		state = Bit.set(state, 4, Math.max(curr, time), 4);
	}

	public void reduceStun(int time) {
		int curr = Bit.get(state, 4, 4);
		state = Bit.set(state, 4, Math.max(0, curr - time), 4);
	}

	public boolean isStasis() {
		return Bit.on(state, 5, 4);
	}

	public int getRemainingStasis() {
		return Bit.get(state, 5, 4);
	}

	public void setStasis(int time) {
		if (popFlag(Flag.NO_STASIS)) return;

		int curr = Bit.get(state, 5, 4);
		state = Bit.set(state, 5, Math.max(curr, time), 4);
	}

	public void reduceStasis(int time) {
		int curr = Bit.get(state, 5, 4);
		state = Bit.set(state, 5, Math.max(0, curr - time), 4);
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

	public Senshi getTarget() {
		return target;
	}

	public int getRemainingTaunt() {
		return Bit.get(state, 6, 4);
	}

	public int getTaunt() {
		int taunt = Bit.get(state, 6, 4);
		if (taunt == 0 || (target == null || target.getSide() == getSide() || target.getIndex() == -1)) {
			state = Bit.set(state, 6, 0, 4);
			target = null;
			taunt = 0;
		}

		return taunt;
	}

	public void setTaunt(Senshi target, int time) {
		if (target == null || popFlag(Flag.NO_TAUNT)) return;

		this.target = target;
		int curr = Bit.get(state, 6, 4);
		state = Bit.set(state, 6, Math.max(curr, time), 4);
	}

	public void reduceTaunt(int time) {
		int curr = Bit.get(state, 6, 4);
		state = Bit.set(state, 6, Math.max(0, curr - time), 4);
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
		return pop ? popFlag(Flag.BLIND) : hasFlag(Flag.BLIND);
	}

	public double getHitChance() {
		double hit = 100;
		if (isBlinded(true)) {
			hit *= 0.75;
		}

		if (hand.getGame().getArena().getField().getType() == FieldType.NIGHT) {
			hit *= 0.8;

			if (hand.getOrigin().synergy() == Race.WEREBEAST) {
				hit += (100 - hit) / 2;
			}
		}

		return hit;
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
		EffectHolder<?> source = (EffectHolder<?>) Utils.getOr(stats.getSource(), this);

		return Utils.getOr(source.getStats().getEffect(), source.getBase().getEffect());
	}

	public boolean hasEffect() {
		return !isSealed() && !getEffect().isBlank();
	}

	public boolean hasAbility() {
		if (getEffect().contains(Trigger.ON_ACTIVATE.name())) {
			return true;
		}

		for (Evogear e : equipments) {
			if (e.getEffect().contains(Trigger.ON_ACTIVATE.name())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean execute(EffectParameters ep) {
		return execute(false, ep);
	}

	public boolean execute(boolean global, EffectParameters ep) {
		if (base.isLocked()) return false;
		else if (popFlag(Flag.NO_EFFECT) || hand.getLockTime(Lock.EFFECT) > 0) return false;

		Trigger trigger = null;
		Senshi s = this;
		boolean targeted = false;

		if (ep.trigger() == Trigger.ON_DEFER) {
			if (ep.size() == 0) return false;

			s = getFrontline();
			if (s == null) return false;
		}

		if (s.equals(ep.source().card())) {
			trigger = ep.source().trigger();
		} else {
			for (Target target : ep.targets()) {
				if (s.equals(target.card())) {
					trigger = target.trigger();
					break;
				}
			}

			if (trigger == null) {
				trigger = ep.trigger();
			}

			targeted = true;
		}

		if ((trigger == Trigger.ON_ACTIVATE && (getCooldown() > 0 || isSupporting()))) return false;

		//Hand other = ep.getHands().get(ep.getOtherSide());
		try {
			base.lock();

			/*if (hero != null) {
				other.setHeroDefense(true);
			}*/

			for (Evogear e : equipments) {
				e.execute(new EffectParameters(trigger, getSide(), ep.source(), ep.targets()));
			}

			if (hasEffect() && getEffect().contains(trigger.name())) {
				if (isStunned() && Calc.chance(25)) {
					Shoukan game = hand.getGame();

					if (!global) {
						game.getChannel().sendMessage(game.getLocale().get("str/effect_stunned", this)).queue();
					}
				} else {
					cachedEffect.forScript(getEffect())
							.withConst("self", this)
							.withConst("game", hand.getGame())
							.withConst("data", stats.getData())
							.withVar("ep", ep)
							.withVar("side", hand.getSide())
							.withVar("props", extractValues(hand.getGame().getLocale(), cachedEffect))
							.withVar("trigger", trigger)
							.run();
				}
			}

			Senshi sup = getSupport();
			if (sup != null && !global) {
				sup.execute(new EffectParameters(Trigger.ON_DEFER, getSide(), ep.source(), ep.targets()));
			}

			for (@Language("Groovy") String curse : stats.getCurses()) {
				if (curse.isBlank() || !curse.contains(trigger.name())) continue;

				Utils.exec(curse, Map.of(
						"self", this,
						"game", hand.getGame(),
						"data", stats.getData(),
						"ep", ep,
						"side", hand.getSide(),
						"props", extractValues(hand.getGame().getLocale(), cachedEffect),
						"trigger", trigger
				));
			}

			if (Utils.equalsAny(trigger, ON_EFFECT_TARGET, ON_DEFEND)) {
				Shoukan game = hand.getGame();

				if (!game.getCurrent().equals(hand)) {
					for (SlotColumn sc : game.getSlots(getSide())) {
						for (Senshi card : sc.getCards()) {
							if (card instanceof CardProxy) {
								EffectParameters params;
								if (targeted) {
									params = new EffectParameters(
											Trigger.ON_TRAP, getSide(),
											card.asSource(ON_TRAP),
											asTarget(trigger, TargetType.ALLY),
											ep.source().toTarget(TargetType.ENEMY)
									);
								} else {
									params = new EffectParameters(
											Trigger.ON_TRAP, getSide(),
											card.asSource(ON_TRAP),
											asTarget(trigger, TargetType.ALLY)
									);
								}

								if (game.activateProxy(card, params)) {
									game.getChannel().sendMessage(game.getLocale().get("str/trap_activation", card)).queue();
								}
							}
						}
					}
				}
			}

			popFlag(Flag.EMPOWERED);
			return true;
		} catch (TargetException e) {
			TargetType type = stats.getData().getEnum(TargetType.class, "targeting");
			if (type != null && trigger == Trigger.ON_ACTIVATE) {
				Shoukan game = hand.getGame();
				game.getChannel().sendMessage(game.getLocale().get("error/target", game.getLocale().get("str/target_" + type))).queue();
			}

			return false;
		} catch (ActivationException e) {
			if (e instanceof SelectionException && trigger != Trigger.ON_ACTIVATE) return false;

			Shoukan game = hand.getGame();
			game.getChannel().sendMessage(game.getLocale().get("error/activation", game.getString(e.getMessage()))).queue();
			return false;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute " + card.getName() + " effect", e);
			return false;
		} finally {
			unlock();
			//other.setHeroDefense(false);
		}
	}

	@Override
	public void executeAssert(Trigger trigger) {
		if (base.isLocked()) return;
		else if (!Utils.equalsAny(trigger, Trigger.ON_INITIALIZE, Trigger.ON_REMOVE)) return;
		else if (!hasEffect() || !getEffect().contains(trigger.name())) return;

		try {
			base.lock();

			Utils.exec(getEffect(), Map.of(
					"self", this,
					"game", hand.getGame(),
					"data", stats.getData(),
					"ep", new EffectParameters(trigger, getSide()),
					"side", hand.getSide(),
					"props", extractValues(hand.getGame().getLocale(), cachedEffect),
					"trigger", trigger
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute " + card.getName() + " effect", e);
		} finally {
			unlock();
		}
	}

	public void unlock() {
		base.unlock();
		for (Evogear e : equipments) {
			e.getBase().unlock();
		}
	}

	public void noEffect(Consumer<Senshi> c) {
		try {
			base.lock();
			c.accept(this);
		} finally {
			base.unlock();
		}
	}

	public boolean isProtected() {
		if (hand != null) {
			Evogear shield = null;
			hand.getGame().trigger(Trigger.ON_EFFECT_TARGET, new Source(this, Trigger.ON_EFFECT_TARGET));
			if (isStasis() || popFlag(Flag.IGNORE_EFFECT)) {
				return true;
			}

			if (!hand.equals(hand.getGame().getCurrent())) {
				for (Evogear e : equipments) {
					if (e.hasCharm(Charm.SHIELD)) {
						shield = e;
					}
				}
			}

			if (shield != null) {
				int charges = shield.getStats().getData().getInt("uses", 0) + 1;
				if (charges >= Charm.SHIELD.getValue(shield.getTier())) {
					hand.getGraveyard().add(shield);
				} else {
					shield.getStats().getData().put("uses", charges);
				}

				return true;
			}
		}

		return false;
	}

	@Override
	public boolean keepOnDestroy() {
		return !isFusion();
	}

	@Override
	public void reset() {
		equipments = new BondedList<>((e, it) -> {
			e.setEquipper(this);
			e.setHand(getHand());
			e.executeAssert(Trigger.ON_INITIALIZE);

			Shoukan game = getHand().getGame();
			game.trigger(Trigger.ON_EQUIP, asSource(Trigger.ON_EQUIP));

			if (e.hasCharm(Charm.TIMEWARP)) {
				int times = Charm.TIMEWARP.getValue(e.getTier());
				for (int i = 0; i < times; i++) {
					game.trigger(Trigger.ON_TURN_BEGIN, asSource(Trigger.ON_TURN_BEGIN));
					game.trigger(Trigger.ON_TURN_END, asSource(Trigger.ON_TURN_END));
				}
			}

			if (e.hasCharm(Charm.CLONE)) {
				game.putAtOpenSlot(getSide(), true,
						withCopy(s -> s.getStats().setAttrMult(-1 + (0.25 * e.getTier())))
				);
			}

			return true;
		}, e -> {
			e.executeAssert(Trigger.ON_REMOVE);
			e.setEquipper(null);
		});
		stats = stats.clone();
		slot = null;
		if (leech != null) {
			leech.getLeeches().remove(this);
		}
		lastInteraction = null;
		cachedEffect = new CachedScriptManager();

		byte base = 0b11;
		base = (byte) Bit.set(base, 4, isSealed());

		state = base;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		BufferedImage out = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		DeckStyling style = deck.getStyling();
		if (isFlipped()) {
			g2d.drawImage(style.getFrame().getBack(deck), 15, 15, null);
		} else {
			Senshi card = Utils.getOr(stats.getDisguise(), this);
			String desc = isSealed() ? "" : card.getDescription(locale);
			BufferedImage img = card.getVanity().drawCardNoBorder(style.isUsingChrome());

			Graph.applyTransformed(g2d, 15, 15, g1 -> {
				g1.setClip(style.getFrame().getBoundary());
				g1.drawImage(img, 0, 0, null);
				g1.setClip(null);

				g1.drawImage(style.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
				g1.drawImage(card.getRace().getIcon(), 190, 12, null);

				g1.setFont(FONT);
				g1.setColor(style.getFrame().getPrimaryColor());
				String name = Graph.abbreviate(g1, card.getVanity().getName(), MAX_NAME_WIDTH);
				Graph.drawOutlinedString(g1, name, 12, 30, 2, style.getFrame().getBackgroundColor());

				if (!desc.isEmpty()) {
					g1.setColor(style.getFrame().getSecondaryColor());
					g1.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 11));

					int y = 276;
					String tags = card.processTags(locale);
					if (tags != null) {
						g1.drawString(tags, 7, 275);
						y += 11;
					}

					JSONObject values = extractValues(locale, cachedEffect);
					Graph.drawMultilineString(g1, desc,
							7, y, 211, 3,
							card.parseValues(g1, deck.getStyling(), values), card.highlightValues(g1, style.getFrame().isLegacy())
					);
				}

				if (!stats.getWrite().isBlank() && getSlot().getIndex() > -1) {
					g1.setColor(Color.ORANGE);
					g1.setFont(Drawable.FONT.deriveFont(15f));

					String str = stats.getWrite();
					FontMetrics fm = g1.getFontMetrics();
					Graph.drawOutlinedString(g1, str,
							225 / 2 - fm.stringWidth(str) / 2, 39 + fm.getHeight() / 2,
							2, Color.BLACK
					);
				}

				if (!hasFlag(Flag.HIDE_STATS)) {
					card.drawCosts(g1);
					if (!isSupporting()) {
						card.drawAttributes(g1, !desc.isEmpty());
					}
				}
			});

			if (hand != null) {
				boolean legacy = hand.getUserDeck().getStyling().getFrame().isLegacy();
				String path = "kawaipon/frames/" + (legacy ? "old" : "new") + "/";

				if (hasFlag(Flag.EMPOWERED)) {
					BufferedImage emp = IO.getResourceAsImage(path + "empowered.png");

					g2d.drawImage(emp, 0, 0, null);
				}

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
			}
		}

		Graph.applyTransformed(g2d, 15, 15, g1 -> {
			if (!isAvailable()) {
				RescaleOp op = new RescaleOp(0.5f, 0, null);
				op.filter(out, out);
			}

			boolean over = false;
			String[] states = {"stasis", "stun", "sleep", "taunt", "defense"};
			int[] timers = {getRemainingStasis(), getRemainingStun(), getRemainingSleep(), getRemainingTaunt()};
			for (int i = 0; i < timers.length; i++) {
				int time = timers[i];

				if (time > 0) {
					over = true;
					BufferedImage overlay = IO.getResourceAsImage("shoukan/states/" + states[i] + ".png");
					g1.drawImage(overlay, 0, 0, null);

					String str = locale.get("str/turns", time);

					g1.setColor(Graph.getColor(overlay).brighter());
					g1.setFont(Drawable.FONT.deriveFont(Drawable.FONT.getSize() * 2f));
					Graph.drawOutlinedString(g1, str,
							225 / 2 - g1.getFontMetrics().stringWidth(str) / 2, 365 - g1.getFontMetrics().getHeight(),
							4, Color.BLACK
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
		return Objects.equals(id, senshi.id)
				&& Objects.equals(card, senshi.card)
				&& Objects.equals(slot, senshi.slot)
				&& race == senshi.race
				&& SERIAL == senshi.SERIAL;
	}

	public int posHash() {
		return Objects.hash(id, slot, isSupporting());
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
		return getVanity().getName();
	}

	public static Senshi getRandom() {
		String id = DAO.queryNative(String.class, "SELECT card_id FROM senshi WHERE NOT has(tags, 'FUSION') ORDER BY RANDOM()");
		if (id == null) return null;

		return DAO.find(Senshi.class, id);
	}

	public static Senshi getRandom(boolean allowFusion, String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM senshi");
		for (String f : filters) {
			query.appendNewLine(f);
		}

		if (!allowFusion) {
			if (filters.length == 0) {
				query.appendNewLine("WHERE NOT has(tags, 'FUSION')");
			} else {
				query.appendNewLine("AND NOT has(tags, 'FUSION')");
			}
		}

		query.appendNewLine("ORDER BY RANDOM()");

		String id = DAO.queryNative(String.class, query.toString());
		if (id == null) return null;

		return DAO.find(Senshi.class, id);
	}

	public static XList<Senshi> getByTag(String... tags) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT by_tag('senshi', ?1)", (Object[]) tags);

		return new XList<>(DAO.queryAll(Senshi.class, "SELECT s FROM Senshi s WHERE s.card.id IN ?1", ids));
	}
}
