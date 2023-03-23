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

package com.kuuhaku.model.common.shoukan;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.exceptions.SelectionException;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.interfaces.shoukan.Proxy;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.*;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.model.records.shoukan.Timed;
import com.kuuhaku.util.*;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.Transient;
import kotlin.Triple;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Hand {
	private final long SERIAL = ThreadLocalRandom.current().nextLong();

	private final String uid;
	private final Shoukan game;
	private final Deck userDeck;
	private final Side side;

	private final BondedList<Drawable<?>> cards = new BondedList<>((d, it) -> {
		if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		d.setHand(this);
		getGame().trigger(Trigger.ON_HAND, d.asSource(Trigger.ON_HAND));

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			Iterator<Evogear> i = s.getEquipments().iterator();
			while (i.hasNext()) {
				it.add(i.next());
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		if (d.isSolid()) {
			getData().put("last_drawn_" + d.getClass().getSimpleName().toLowerCase(), d);
		}

		d.setSlot(null);

		if (d instanceof Proxy<?> p) {
			Senshi s = (Senshi) p;
			d.reset();
			it.add(p.getOriginal());
			return !s.getStats().popFlag(Flag.BOUND);
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.getStats().popFlag(Flag.BOUND);
	});
	private final BondedList<Drawable<?>> deck = new BondedList<>((d, it) -> {
		if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		d.setHand(this);
		getGame().trigger(Trigger.ON_DECK, d.asSource(Trigger.ON_DECK));

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			Iterator<Evogear> i = s.getEquipments().iterator();
			while (i.hasNext()) {
				it.add(i.next());
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();

		if (d instanceof Proxy<?> p) {
			Senshi s = (Senshi) p;
			d.reset();
			it.add(p.getOriginal());
			return !s.getStats().popFlag(Flag.BOUND);
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.getStats().popFlag(Flag.BOUND);
	});
	private final BondedList<Drawable<?>> graveyard = new BondedList<>((d, it) -> {
		if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		if (d instanceof Senshi s) {
			if (getGame().getCurrentSide() != getSide() && Calc.chance(s.getDodge() / 2d)) {
				getGame().getChannel().sendMessage(getGame().getLocale().get("str/avoid_destruction", s)).queue();
				return false;
			} else if (s.popFlag(Flag.NO_DEATH) || s.hasCharm(Charm.WARDING, true)) {
				return false;
			}
		}

		d.setHand(this);
		getGame().trigger(Trigger.ON_GRAVEYARD, d.asSource(Trigger.ON_GRAVEYARD));
		if (d instanceof Senshi s && s.popFlag(Flag.NO_DEATH)) {
			return false;
		}

		if (getGame().getCurrentSide() != getSide()) {
			Hand op = getOther();

			op.addKill();
			if (op.getKills() % 7 == 0 && op.getOrigin().synergy() == Race.SHINIGAMI) {
				getGame().getArena().getBanned().add(d);
				return false;
			}
		}

		if (d instanceof Senshi s) {
			if (s.getLastInteraction() != null) {
				getGame().trigger(Trigger.ON_KILL, s.getLastInteraction().asSource(Trigger.ON_KILL), s.asTarget(Trigger.NONE));

				Hand other = d.getHand().getOther();
				other.addKill();

				if (getGame().getArcade() == Arcade.DECK_ROYALE) {
					other.manualDraw(3);
				}
			}

			if (!s.getEquipments().isEmpty()) {
				Iterator<Evogear> i = s.getEquipments().iterator();
				while (i.hasNext()) {
					it.add(i.next());
				}
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();

		if (d.getHand().getOrigin().synergy() == Race.REBORN && Calc.chance(5)) {
			cards.add(d.copy());
			return false;
		}

		if (d instanceof Proxy<?> p) {
			Senshi s = (Senshi) p;
			d.reset();
			it.add(p.getOriginal());
			return !s.getStats().popFlag(Flag.BOUND);
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.getStats().popFlag(Flag.BOUND);
	});
	private final BondedList<Drawable<?>> discard = new BondedList<>((d, it) -> {
		if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		d.setHand(this);
		getData().put("last_discarded", d);
		getGame().trigger(Trigger.ON_DISCARD, d.asSource(Trigger.ON_DISCARD));
		d.setAvailable(false);

		if (d instanceof Proxy<?> p) {
			Senshi s = (Senshi) p;
			d.reset();
			it.add(p.getOriginal());
			return !s.getStats().popFlag(Flag.BOUND);
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.getStats().popFlag(Flag.BOUND);
	});
	private final Set<Timed<Lock>> locks = new HashSet<>();
	private final Set<EffectHolder<?>> leeches = new HashSet<>();

	private final RegDeg regdeg = new RegDeg(this);
	private final JSONObject data = new JSONObject();

	private String name;

	private Origin origin;
	private BaseValues base;
	private int hp;
	private int mp;
	private int originHash;

	private transient Account account;
	private transient String lastMessage;
	private transient String defeat;
	private transient int hpDelta = 0;

	@Transient
	private int state = 0b100;
	/*
	0x000 FFFF F
	      │└┤│ └ 1111
	      │ ││   │││└ forfeit
	      │ ││   ││└─ destiny
	      │ ││   │└── reroll
	      │ ││   └─── empower
	      │ │└ (0 - 15) cooldown
	      │ └─ (0 - 255) kills
	      └─── (0 - 15) chain reduction
	 */

	private transient Triple<List<Drawable<?>>, Boolean, CompletableFuture<Drawable<?>>> selection = null;

	public Hand(String uid, Shoukan game, Side side) {
		this.uid = uid;
		this.game = game;
		this.userDeck = DAO.find(Account.class, uid).getCurrentDeck();
		if (game.getArcade() != Arcade.CARDMASTER) {
			if (userDeck == null) {
				throw new GameReport(GameReport.NO_DECK, uid);
			} else if (!(userDeck.validateSenshi() && userDeck.validateEvogear() && userDeck.validateFields())) {
				throw new GameReport(GameReport.INVALID_DECK, uid);
			}
		}

		this.side = side;
		this.origin = userDeck.getOrigins();
		this.base = getBase();
		this.hp = base.hp();

		if (game.getArcade() == Arcade.CARDMASTER) {
			List<List<? extends Drawable<?>>> cards = new ArrayList<>();
			cards.add(DAO.queryAll(Senshi.class, "SELECT s FROM Senshi s WHERE has(s.base.tags, 'FUSION') = FALSE"));
			cards.add(DAO.queryAll(Evogear.class, "SELECT e FROM Evogear e WHERE e.base.mana > 0"));
			cards.add(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE NOT f.effect"));

			deck.addAll(
					cards.parallelStream()
							.flatMap(List::stream)
							.map(d -> d.copy())
							.peek(d -> {
								if (d instanceof Field f && origin.synergy() == Race.PIXIE) {
									Utils.shufflePairs(f.getModifiers());
								} else if (d instanceof Senshi s && origin.hasMinor(Race.DIVINITY) && !s.hasEffect()) {
									s.getStats().setSource(
											Senshi.getRandom(false,
													"WHERE effect IS NOT NULL",
													"AND mana = " + s.getBase().getMana()
											)
									);
								}
							})
							.collect(Utils.toShuffledList())
			);
			return;
		}

		deck.addAll(
				Stream.of(userDeck.getSenshi(), userDeck.getEvogear(), userDeck.getFields())
						.parallel()
						.flatMap(List::stream)
						.map(d -> d.copy())
						.peek(d -> {
							if (d instanceof Field f && origin.synergy() == Race.PIXIE) {
								Utils.shufflePairs(f.getModifiers());
							} else if (d instanceof Senshi s && origin.hasMinor(Race.DIVINITY) && !s.hasEffect()) {
								s.getStats().setSource(
										Senshi.getRandom(false,
												"WHERE effect IS NOT NULL",
												"AND mana = " + s.getBase().getMana()
										)
								);
							}
						})
						.collect(Utils.toShuffledList())
		);
	}

	public String getUid() {
		return uid;
	}

	public User getUser() {
		return Main.getApp().getUserById(uid);
	}

	public Shoukan getGame() {
		return game;
	}

	public Deck getUserDeck() {
		return userDeck;
	}

	public Side getSide() {
		return side;
	}

	public Hand getOther() {
		return game.getHands().get(side.getOther());
	}

	public Origin getOrigin() {
		return origin;
	}

	public void setOrigin(Origin origin) {
		this.origin = origin;
	}

	public BondedList<Drawable<?>> getCards() {
		cards.removeIf(d -> !equals(d.getHand()));

		return cards;
	}

	public int getHandCount() {
		return (int) cards.stream().filter(Drawable::isSolid).count();
	}

	public int getRemainingDraws() {
		return Math.max(0, base.handCapacity().get() - getHandCount());
	}

	public BondedList<Drawable<?>> getRealDeck() {
		deck.removeIf(d -> !equals(d.getHand()));

		return deck;
	}

	public BondedList<Drawable<?>> getDeck() {
		if (getLockTime(Lock.DECK) > 0) {
			return new BondedList<>();
		}

		return getRealDeck();
	}

	public void manualDraw(int value) {
		if (deck.isEmpty()) return;

		if (cards.stream().noneMatch(d -> d instanceof Senshi)) {
			for (int i = 0; i < deck.size() && value > 0; i++) {
				if (deck.get(i) instanceof Senshi) {
					if (getOther().getOrigin().synergy() == Race.IMP) {
						modHP(-25);
					}

					Drawable<?> d = deck.remove(i);
					d.setSolid(true);

					cards.add(d);
					getGame().trigger(Trigger.ON_DRAW);
					value--;
					break;
				}
			}
		}

		for (int i = 0; i < value && !deck.isEmpty(); i++) {
			Drawable<?> d = deck.removeFirst();

			if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
				modHP(50);
			}
			if (getOther().getOrigin().synergy() == Race.IMP) {
				modHP(-25);
			}

			if (d != null) {
				d.setSolid(true);
				cards.add(d);
				getGame().trigger(Trigger.ON_DRAW);
			}
		}
	}

	public Drawable<?> draw() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		if (deck.isEmpty()) return null;

		Drawable<?> d = deck.removeFirst();

		if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
			modHP(50);
		}
		if (getOther().getOrigin().synergy() == Race.IMP) {
			modHP(-25);
		}

		if (d != null) {
			d.setSolid(true);
			cards.add(d);
			getGame().trigger(Trigger.ON_DRAW);
		}

		return d;
	}

	public List<Drawable<?>> draw(int value) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return List.of();

		List<Drawable<?>> out = new ArrayList<>();
		for (int i = 0; i < value; i++) {
			Drawable<?> d = draw();
			if (d == null) return out;

			out.add(d);
		}

		return out;
	}

	public Drawable<?> draw(String card) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			Drawable<?> d = deck.get(i);
			if (d.getCard().getId().equalsIgnoreCase(card)) {
				if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
					modHP(50);
				}
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-25);
				}

				Drawable<?> out = deck.remove(i);
				out.setSolid(true);

				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> draw(Race race) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Senshi s && s.getRace().isRace(race)) {
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-25);
				}

				Drawable<?> out = deck.remove(i);
				out.setSolid(true);

				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> draw(Predicate<Drawable<?>> cond) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			Drawable<?> d = deck.get(i);
			if (cond.test(d)) {
				if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
					modHP(50);
				}
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-25);
				}

				Drawable<?> out = deck.remove(i);
				out.setSolid(true);

				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> drawSenshi() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Senshi s) {
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-25);
				}

				Drawable<?> out = deck.remove(i);
				out.setSolid(true);

				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> drawEvogear() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Evogear e) {
				if (origin.synergy() == Race.EX_MACHINA && !e.isSpell()) {
					modHP(50);
				}
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-25);
				}

				Drawable<?> out = deck.remove(i);
				out.setSolid(true);

				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> drawEquipment() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Evogear e && !e.isSpell()) {
				if (origin.synergy() == Race.EX_MACHINA) {
					modHP(50);
				}

				Drawable<?> out = deck.remove(i);
				out.setSolid(true);

				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> drawSpell() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Evogear e && e.isSpell()) {
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-25);
				}

				Drawable<?> out = deck.remove(i);
				out.setSolid(true);

				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public void rerollHand() {
		int i = 0;
		Iterator<Drawable<?>> it = cards.iterator();
		while (it.hasNext()) {
			Drawable<?> card = it.next();
			if (card.isAvailable()) {
				deck.add(card);
				it.remove();
				i++;
			}
		}

		Collections.shuffle(deck);
		manualDraw(i);
	}

	public BondedList<Drawable<?>> getGraveyard() {
		graveyard.removeIf(d -> !equals(d.getHand()) || !d.keepOnDestroy());

		return graveyard;
	}

	public BondedList<Drawable<?>> getDiscard() {
		discard.removeIf(d -> !cards.contains(d) || !d.keepOnDestroy());

		return discard;
	}

	public void flushDiscard() {
		cards.removeIf(d -> !d.isAvailable());
		graveyard.addAll(discard);
		discard.clear();
	}

	public Set<Timed<Lock>> getLocks() {
		return locks;
	}

	public void modLockTime(Lock lock, int time) {
		if (time == 0) return;

		Iterator<Timed<Lock>> it = locks.iterator();
		while (it.hasNext()) {
			Timed<Lock> lk = it.next();

			if (lk.obj() == lock) {
				if (lk.time().addAndGet(time) <= 0) {
					it.remove();
				}

				return;
			}
		}

		if (time > 0) {
			locks.add(new Timed<>(lock, time));
		}
	}

	public int getLockTime(Lock lock) {
		return locks.stream()
				.filter(t -> t.obj().equals(lock))
				.map(Timed::time)
				.mapToInt(AtomicInteger::get)
				.sum();
	}

	public Set<EffectHolder<?>> getLeeches() {
		leeches.removeIf(e -> !Objects.equals(e.getLeech(), this));

		return leeches;
	}

	public BaseValues getBase() {
		if (originHash != origin.hashCode()) {
			base = userDeck.getBaseValues(this);
			originHash = origin.hashCode();
		}

		return base;
	}

	public String getName() {
		if (name == null) {
			name = Utils.getOr(DAO.find(Account.class, uid).getName(), "???");
		}

		return name;
	}

	public int getHP() {
		return hp;
	}

	public void setHP(int hp) {
		this.hp = Utils.clamp(hp, 0, base.hp() * 2);
	}

	public void modHP(int value) {
		modHP(value, false);
	}

	public void modHP(int value, boolean pure) {
		if (value == 0) return;
		else if (game.getArcade() == Arcade.OVERCHARGE) {
			value *= Math.min(0.5 + 0.5 * (Math.ceil(game.getTurn() / 2d) / 10), 1);
		} else if (game.getArcade() == Arcade.DECAY) {
			if (value > 0) {
				value /= 2;
			} else {
				value *= 1.5;
			}
		}

		int before = hp;

		if (!pure) {
			if (origin.major() == Race.HUMAN && value > 0) {
				value *= 1.25;
			} else if (origin.hasMinor(Race.HUMAN) && value < 0) {
				value *= 1 - Math.min(game.getTurn() * 0.01, 0.75);
			}

			if (origin.synergy() == Race.POSSESSED && value > 0) {
				value *= 1 + getOther().getGraveyard().size() * 0.05;
			} else if (origin.synergy() == Race.PRIMAL && value < 0) {
				int degen = value / 10;
				if (degen < 0) {
					regdeg.add(degen);
					value -= degen;
				}
			}

			int dot = regdeg.peek();
			int quart = value / 4;
			if (dot > 0 && value < 0) {
				value -= quart + regdeg.reduce(Degen.class, quart);
			} else if (dot < 0 && value > 0) {
				value -= quart - regdeg.reduce(Regen.class, quart);
			}

			double prcnt = getHPPrcnt();
			if (origin.demon()) {
				prcnt = Math.min(prcnt, 0.5);
			}

			if (this.hp + value <= 0 && prcnt > 1 / 3d) {
				if (prcnt > 2 / 3d || Calc.chance(prcnt * 100)) {
					this.hp = 1;
					return;
				}
			}
		}

		this.hp = Utils.clamp(this.hp + value, 0, base.hp() * 2);

		if (!pure) {
			hpDelta = this.hp - before;
			if (hpDelta < 0) {
				game.trigger(Trigger.ON_DAMAGE, side);

				if (origin.synergy() == Race.VIRUS) {
					modMP((int) -(hpDelta * 0.0025));
				} else if (origin.synergy() == Race.TORMENTED) {
					getOther().modHP((int) (hpDelta * 0.01));
				}
			} else if (hpDelta > 0) {
				game.trigger(Trigger.ON_HEAL, side);
			}
		}
	}

	public boolean consumeHP(int value) {
		if (hp < value) return false;

		this.hp = Math.max(0, this.hp - Math.max(0, value));
		return true;
	}

	public double getHPPrcnt() {
		return hp / (double) base.hp();
	}

	public int getHpDelta() {
		return hpDelta;
	}

	public boolean isLowLife() {
		return origin.demon() || getHPPrcnt() < 0.5;
	}

	public boolean isCritical() {
		return getHPPrcnt() < 0.25;
	}

	public RegDeg getRegDeg() {
		return regdeg;
	}

	public void applyVoTs() {
		int val = regdeg.next();

		if (val < 0 && origin.major() == Race.HUMAN) {
			val /= 2;
		}

		modHP(val);
	}

	public JSONObject getData() {
		return data;
	}

	public int getMP() {
		if (origin.major() == Race.DEMON) {
			return (int) Math.max(0, hp / (base.hp() * 0.08) - 1);
		}

		return mp;
	}

	public void setMP(int mp) {
		if (origin.major() == Race.DEMON) {
			setHP((int) ((mp + 1) * (base.hp() * 0.08)));
			return;
		}

		this.mp = Utils.clamp(mp, 0, 99);
	}

	public void modMP(int value) {
		if (origin.major() == Race.DEMON) {
			modHP((int) (value * (base.hp() * 0.08)));
			return;
		}

		this.mp = Utils.clamp(this.mp + value, 0, 99);
	}

	public boolean consumeMP(int value) {
		if (origin.synergy() == Race.ESPER && Calc.chance(3)) return true;
		else if (origin.major() == Race.DEMON) {
			return consumeHP((int) (value * (base.hp() * 0.08)));
		} else if (this.mp < value) return false;

		this.mp = Utils.clamp(this.mp - value, 0, 99);
		return true;
	}

	public List<Drawable<?>> consumeSC(int value) {
		List<Drawable<?>> consumed = new ArrayList<>();

		for (int i = 0; i < value && !discard.isEmpty(); i++) {
			Drawable<?> card = discard.remove(0);

			consumed.add(card);
			game.getArena().getBanned().add(card);
			cards.remove(card);
		}

		return consumed;
	}

	public Account getAccount() {
		if (account == null) {
			account = DAO.find(Account.class, uid);
		}

		return account;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}

	public boolean isForfeit() {
		return Bit.on(state, 0);
	}

	public void setForfeit(boolean forfeit) {
		state = Bit.set(state, 0, forfeit);
	}

	public boolean hasUsedDestiny() {
		return Bit.on(state, 1);
	}

	public void setUsedDestiny(boolean usedDestiny) {
		state = Bit.set(state, 1, usedDestiny);
	}

	public boolean hasRerolled() {
		return Bit.on(state, 2);
	}

	public void setRerolled(boolean rerolled) {
		state = Bit.set(state, 2, rerolled);
	}

	public boolean isEmpowered() {
		return Bit.on(state, 3);
	}

	public void setEmpowered(boolean empowered) {
		state = Bit.set(state, 3, empowered);
	}

	public boolean isDefeated() {
		return defeat != null;
	}

	public String getDefeat() {
		return defeat;
	}

	public void setDefeat(String defeat) {
		this.defeat = defeat;
	}

	public int getOriginCooldown() {
		return Bit.get(state, 1, 4);
	}

	public void setOriginCooldown(int time) {
		int curr = Bit.get(state, 1, 4);
		state = Bit.set(state, 1, Math.max(curr, time), 4);
	}

	public void reduceOriginCooldown(int time) {
		int curr = Bit.get(state, 1, 4);
		state = Bit.set(state, 1, Math.max(0, curr - time), 4);
	}

	public int getKills() {
		return Bit.get(state, 1, 8);
	}

	public void addKill() {
		int curr = Bit.get(state, 1, 8);
		state = Bit.set(state, 1, curr + 1, 8);
	}

	public int getChainReduction() {
		return Bit.get(state, 4, 4);
	}

	public void addChain() {
		int curr = Bit.get(state, 4, 4);
		state = Bit.set(state, 4, curr + 1, 4);
	}

	public void resetChain() {
		state = Bit.set(state, 4, 0, 4);
	}

	public BufferedImage render() {
		return render(cards);
	}

	public BufferedImage render(List<Drawable<?>> cards) {
		if (cards.isEmpty()) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		BufferedImage bi = new BufferedImage((Drawable.SIZE.width + 20) * 5, (100 + Drawable.SIZE.height) * (int) Math.ceil(cards.size() / 5d), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 90));

		for (int i = 0; i < cards.size(); i++) {
			int offset = bi.getWidth() / 2 - (Drawable.SIZE.width + 20) * Math.min(cards.size() - (i / 5) * 5, 5) / 2;
			int x = offset + 10 + (Drawable.SIZE.width + 10) * (i % 5);
			int y = 100 + (100 + Drawable.SIZE.height) * (i / 5);

			Drawable<?> d = cards.get(i);
			boolean ally = equals(d.getHand());

			if (getLockTime(Lock.BLIND) > 0 && ally) {
				g2d.drawImage(userDeck.getStyling().getFrame().getBack(userDeck), x, y, null);
			} else {
				g2d.drawImage(d.render(game.getLocale(), userDeck), x, y, null);
			}

			if (d.isAvailable() && ally) {
				Graph.drawOutlinedString(g2d, String.valueOf(i + 1),
						x + (Drawable.SIZE.width / 2 - g2d.getFontMetrics().stringWidth(String.valueOf(i + 1)) / 2), y - 10,
						6, Color.BLACK
				);
			}

			if (!ally) {
				Graph.applyTransformed(g2d, x + 15, y + 15, g -> {
					g.setClip(userDeck.getStyling().getFrame().getBoundary());
					g.drawImage(IO.getResourceAsImage("shoukan/states/sight.png"), 0, 0, null);
				});
			}

			if ((d instanceof Senshi s && s.hasFlag(Flag.EMPOWERED)) || (d instanceof Evogear e && e.getStats().hasFlag(Flag.EMPOWERED))) {
				boolean legacy = userDeck.getStyling().getFrame().isLegacy();
				BufferedImage emp = IO.getResourceAsImage("kawaipon/frames/" + (legacy ? "old" : "new") + "/empowered.png");

				g2d.drawImage(emp, x, y, null);
			}
		}

		g2d.dispose();

		return bi;
	}

	public void showHand() {
		showHand(this);
	}

	public void showHand(Hand hand) {
		getUser().openPrivateChannel()
				.flatMap(chn -> chn.sendFiles(FileUpload.fromData(IO.getBytes(render(hand.getCards()), "png"), "hand.png")))
				.queue(m -> {
					if (equals(hand)) {
						if (lastMessage != null) {
							m.getChannel().retrieveMessageById(lastMessage)
									.flatMap(Objects::nonNull, Message::delete)
									.queue(null, Utils::doNothing);
						}

						lastMessage = m.getId();
					}
				}, Utils::doNothing);
	}

	public void showCards(List<Drawable<?>> cards) {
		getUser().openPrivateChannel()
				.flatMap(chn -> chn.sendFiles(FileUpload.fromData(IO.getBytes(render(cards), "png"), "cards.png")))
				.queue(null, Utils::doNothing);
	}

	public Drawable<?> requestChoice(List<Drawable<?>> cards) {
		return requestChoice(cards, false);
	}

	public Drawable<?> requestChoice(List<Drawable<?>> cards, boolean hide) {
		if (selection != null) {
			try {
				selection.getThird().get();
			} catch (ExecutionException | InterruptedException e) {
				throw new SelectionException("err/pending_selection");
			}
		}

		cards = cards.stream().filter(Objects::nonNull).toList();
		if (cards.isEmpty()) throw new ActivationException("err/empty_selection");

		selection = new Triple<>(cards, hide, new CompletableFuture<>());

		Message msg = Pages.subGet(getUser().openPrivateChannel().flatMap(chn -> chn.sendFiles(FileUpload.fromData(IO.getBytes(renderChoices(), "png"), "choices.png"))));

		game.getChannel().sendMessage(game.getLocale().get("str/selection_sent")).queue();
		try {
			return selection.getThird().thenApply(d -> {
				msg.delete().queue(null, Utils::doNothing);
				selection = null;

				return d;
			}).get();
		} catch (ExecutionException | InterruptedException e) {
			throw new SelectionException("err/pending_selection");
		}
	}

	public void requestChoice(List<Drawable<?>> cards, ThrowingConsumer<Drawable<?>> act) {
		Drawable<?> d = requestChoice(cards);
		if (d == null) return;

		act.accept(d);
	}

	public void requestChoice(List<Drawable<?>> cards, boolean hide, ThrowingConsumer<Drawable<?>> act) {
		Drawable<?> d = requestChoice(cards, hide);
		if (d == null) return;

		act.accept(d);
	}

	public void requestChoice(Predicate<Drawable<?>> cond, ThrowingConsumer<Drawable<?>> act) {
		Drawable<?> d = requestChoice(cards.stream().filter(cond).toList());
		if (d == null) return;

		act.accept(d);
	}

	public BufferedImage renderChoices() {
		List<Drawable<?>> cards = selection.getFirst();
		if (cards.isEmpty()) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		BufferedImage bi = new BufferedImage((Drawable.SIZE.width + 20) * 5, 100 + (100 + Drawable.SIZE.height) * (int) Math.ceil(cards.size() / 5d), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 80));
		g2d.translate(0, 100);

		String str = game.getLocale().get("str/select_a_card");
		Graph.drawOutlinedString(g2d, str,
				bi.getWidth() / 2 - g2d.getFontMetrics().stringWidth(str) / 2, -10,
				6, Color.BLACK
		);

		for (int i = 0; i < cards.size(); i++) {
			int offset = bi.getWidth() / 2 - (Drawable.SIZE.width + 20) * Math.min(cards.size() - (i / 5) * 5, 5) / 2;
			int x = offset + 10 + (Drawable.SIZE.width + 10) * (i % 5);
			int y = 100 + (100 + Drawable.SIZE.height) * (i / 5);

			Drawable<?> d = cards.get(i);
			Deck deck = Utils.getOr(d.getHand(), this).userDeck;

			if (selection.getSecond()) {
				g2d.drawImage(deck.getStyling().getFrame().getBack(deck), x, y, null);
			} else {
				g2d.drawImage(d.render(game.getLocale(), deck), x, y, null);
			}

			if (d.isAvailable()) {
				Graph.drawOutlinedString(g2d, String.valueOf(i + 1),
						x + (Drawable.SIZE.width / 2 - g2d.getFontMetrics().stringWidth(String.valueOf(i + 1)) / 2), y - 10,
						6, Color.BLACK
				);
			}
		}

		g2d.dispose();

		return bi;
	}

	public Triple<List<Drawable<?>>, Boolean, CompletableFuture<Drawable<?>>> getSelection() {
		return selection;
	}

	public boolean selectionPending() {
		return selection != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Hand hand = (Hand) o;
		return SERIAL == hand.SERIAL && Objects.equals(uid, hand.uid) && side == hand.side && Objects.equals(origin, hand.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, uid, side, origin);
	}
}
