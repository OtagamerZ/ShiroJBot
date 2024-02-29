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

package com.kuuhaku.model.common.shoukan;

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
import com.kuuhaku.model.common.SupplyChain;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.*;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.SelectionAction;
import com.kuuhaku.model.records.SelectionCard;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.model.records.shoukan.Timed;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.Transient;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Hand {
	private final long SERIAL = ThreadLocalRandom.current().nextLong();

	private final Shoukan game;
	private final Deck userDeck;
	private final Side side;
	private final HandExtra stats = new HandExtra();
	private final BondedList<Drawable<?>> cards = new BondedList<>((d, it) -> {
		if (getCards().contains(d)) return false;
		else if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
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
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.hasFlag(Flag.BOUND, true);
	});
	private final BondedList<Drawable<?>> deck = new BondedList<>((d, it) -> {
		if (d.isEthereal() || getRealDeck().contains(d)) return false;
		else if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
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
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.hasFlag(Flag.BOUND, true);
	});
	private final BondedList<Drawable<?>> graveyard = new BondedList<>((d, it) -> {
		if (d.isEthereal() || getGraveyard().contains(d)) return false;
		else if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		if (d instanceof Senshi s) {
			if (getGame().getCurrentSide() != getSide() && getGame().chance(s.getDodge() / 2d)) {
				getGame().getChannel().sendMessage(getGame().getString("str/avoid_destruction", s)).queue();
				return false;
			} else if (s.hasFlag(Flag.NO_DEATH, true) || s.hasCharm(Charm.WARDING, true)) {
				return false;
			}
		}

		d.setHand(this);
		getGame().trigger(Trigger.ON_GRAVEYARD, d.asSource(Trigger.ON_GRAVEYARD));
		if (d instanceof Senshi s) {
			if (s.hasFlag(Flag.NO_DEATH, true)) {
				return false;
			}

			if (s.getLastInteraction() != null) {
				getGame().trigger(Trigger.ON_KILL, s.getLastInteraction().asSource(Trigger.ON_KILL), s.asTarget());
				if (s.hasFlag(Flag.NO_DEATH, true)) {
					return false;
				}

				Hand op = getOther();
				op.addKill();

				if (op.getKills() % 7 == 0 && op.getOrigins().synergy() == Race.SHINIGAMI) {
					for (Drawable<?> r : op.getDeck()) {
						if (r instanceof EffectHolder<?> eh) {
							ValueMod vm = eh.getStats().getCostMult().get(getGame().getArena().DEFAULT_FIELD);
							eh.getStats().getCostMult().set(getGame().getArena().DEFAULT_FIELD, Math.max(vm.getValue() - 0.1, -0.5));
						}
					}


					getGame().getArena().getBanned().add(s);
				} else if (op.getOrigins().synergy() == Race.REAPER) {
					op.getDiscard().add(d.copy());
				}

				if (getOrigins().synergy() == Race.ESPER) {
					getCards().add(s.withCopy(c -> c.setEthereal(true)));
				}

				if (getGame().getArcade() == Arcade.DECK_ROYALE) {
					op.manualDraw(3);
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

		if (d instanceof Proxy<?> p) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.hasFlag(Flag.BOUND, true);
	});
	private final BondedList<Drawable<?>> discard = new BondedList<>((d, it) -> {
		if (d.isEthereal() || getDiscard().contains(d)) return false;
		else if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		d.setHand(this);
		d.setAvailable(false);
		getData().put("last_discarded", d);
		getGame().trigger(Trigger.ON_DISCARD, d.asSource(Trigger.ON_DISCARD));

		if (d instanceof Proxy<?> p) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.hasFlag(Flag.BOUND, true);
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
	private transient int mpDelta = 0;
	private transient boolean preventAction = false;
	private transient boolean reached = false;

	@Transient
	private int state = 0b100;
	/*
	0x000 FFFF FF
	      │└┤│ └┴ 0001 1111
	      │ ││       │ │││└ forfeit
	      │ ││       │ ││└─ destiny
	      │ ││       │ │└── reroll
	      │ ││       │ └─── can attack
	      │ ││       └ has summoned
	      │ │└ (0 - 15) cooldown
	      │ └─ (0 - 255) kills
	      └─── (0 - 15) chain reduction
	 */

	private transient SelectionAction selection = null;

	public Hand(Deck deck) {
		this.game = null;
		this.userDeck = deck;
		this.side = Side.BOTTOM;
		this.origin = userDeck.getOrigins();
		this.base = getBase();
		this.hp = base.hp();
		this.mp = base.mpGain().get();
	}

	public Hand(String uid, Shoukan game, Side side) {
		this.game = game;
		this.userDeck = DAO.find(Account.class, uid).getCurrentDeck();
		if (userDeck == null) {
			throw new GameReport(GameReport.NO_DECK, uid);
		}
		this.userDeck.calcStats();

		if (game.getArcade() != Arcade.CARDMASTER && !game.isSingleplayer() && !Account.hasRole(uid, false, Role.TESTER)) {
			if (!(userDeck.validateSenshi() && userDeck.validateEvogear() && userDeck.validateFields())) {
				throw new GameReport(GameReport.INVALID_DECK, uid);
			}
		}

		this.side = side;
		this.origin = userDeck.getOrigins();
		this.base = getBase();
		this.hp = base.hp();

		if (origin.major() == Race.UNDEAD) {
			regdeg.add(base.hp() / 2);
		}

		if (origin.synergy() == Race.REBORN) {
			for (int i = 0; i < 3; i++) {
				deck.add(DAO.find(Evogear.class, "REBIRTH"));
			}
		}

		Stream<List<? extends Drawable<?>>> toAdd;
		if (game.getArcade() == Arcade.CARDMASTER) {
			List<List<? extends Drawable<?>>> cards = new ArrayList<>();
			cards.add(DAO.queryAll(Senshi.class, "SELECT s FROM Senshi s WHERE get_rarity_index(s.card.rarity) BETWEEN 1 AND 5 AND has(s.base.tags, 'FUSION') = FALSE"));
			cards.add(DAO.queryAll(Evogear.class, "SELECT e FROM Evogear e WHERE e.base.mana > 0"));
			cards.add(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE NOT f.effect"));

			toAdd = cards.stream();
		} else {
			toAdd = Stream.of(userDeck.getSenshi(), userDeck.getEvogear(), userDeck.getFields());
		}

		deck.addAll(toAdd.parallel().flatMap(List::stream).map(d -> d.copy()).peek(d -> {
			if (d instanceof Senshi s && origin.synergy() == Race.ELDRITCH && !s.hasEffect()) {
				s.getStats().setSource(Senshi.getRandom(game.getRng(), "WHERE effect IS NOT NULL", "AND mana = " + s.getBase().getMana()));
			}
		}).toList());

		Utils.shuffle(deck, game.getRng());
	}

	public String getUid() {
		return userDeck.getAccount().getUid();
	}

	public User getUser() {
		return Main.getApp().getUserById(getUid());
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
		if (game == null) return null;
		return game.getHands().get(side.getOther());
	}

	public Archetype getArchetype() {
		return userDeck.getArchetype();
	}

	public void loadArchetype() {
		userDeck.getArchetype().execute(this);
	}

	public Origin getOrigins() {
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

	public List<Drawable<?>> manualDraw(int value) {
		if (deck.isEmpty() || value <= 0) return List.of();

		if (cards.stream().noneMatch(d -> d instanceof Senshi)) {
			Senshi out = (Senshi) deck.removeFirst(d -> d instanceof Senshi);
			if (out != null) {
				addToHand(out, true);
				value--;
			}
		}

		List<Drawable<?>> out = new ArrayList<>();
		for (int i = 0; i < Math.min(value, deck.size()); i++) {
			Drawable<?> d = deck.removeFirst();
			if (d != null) {
				out.add(addToHand(d, true));
				value--;
			}
		}

		return out;
	}

	public Drawable<?> draw() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Drawable<?> out = deck.removeFirst();
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
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
		Drawable<?> out = deck.removeFirst(d -> d.getCard().getId().equalsIgnoreCase(card));
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
	}

	public Drawable<?> draw(Race race) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Drawable<?> out = deck.removeFirst(d -> d instanceof Senshi s && s.getRace() == race);
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
	}

	public Drawable<?> draw(Predicate<Drawable<?>> cond) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Drawable<?> out = deck.removeFirst(cond);
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
	}

	public Drawable<?> draw(Drawable<?> card) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		if (deck.remove(card)) {
			return addToHand(card, false);
		}

		return null;
	}

	public Drawable<?> drawSenshi() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Senshi out = (Senshi) deck.removeFirst(d -> d instanceof Senshi);
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
	}

	public Drawable<?> drawEvogear() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Evogear out = (Evogear) deck.removeFirst(d -> d instanceof Evogear);
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
	}

	public Drawable<?> drawEquipment() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Evogear out = (Evogear) deck.removeFirst(d -> d instanceof Evogear e && !e.isSpell());
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
	}

	public Drawable<?> drawSpell() {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Evogear out = (Evogear) deck.removeFirst(d -> d instanceof Evogear e && e.isSpell());
		if (out != null) {
			return addToHand(out, false);
		}

		return null;
	}

	private Drawable<?> addToHand(Drawable<?> out, boolean manual) {
		return addToHand(out, manual, false);
	}

	private Drawable<?> addToHand(Drawable<?> out, boolean manual, boolean oriSkip) {
		if (!oriSkip) {
			if (out instanceof Evogear e && !e.isSpell()) {
				if (origin.synergy() == Race.EX_MACHINA) {
					regdeg.add(500);
				} else if (origin.synergy() == Race.MUMMY && out.getCurses().isEmpty()) {
					Evogear curse = Evogear.getByTag(game.getRng(), "MUMMY_CURSE").getRandom();

					requestChoice(
							List.of(
									new SelectionCard(out, false),
									new SelectionCard(out.withCopy(c -> {
										((Evogear) c).getStats().getAttrMult().set(0.5);
										c.getCurses().add(curse.getEffect());
									}), false)
							),
							ds -> addToHand(ds.getFirst(), false, true)
					);
				}
			}

			if (getOther().getOrigins().synergy() == Race.IMP) {
				modHP(-75);
			}
		}

		out.setSolid(true);
		cards.add(out);

		getGame().trigger(Trigger.ON_DRAW, side);
		if (manual) {
			getGame().trigger(Trigger.ON_MANUAL_DRAW, out.asSource(Trigger.ON_MANUAL_DRAW));
		} else {
			getGame().trigger(Trigger.ON_MAGIC_DRAW, out.asSource(Trigger.ON_MAGIC_DRAW));
		}

		return out;
	}

	public void rerollHand() {
		int i = 0;
		Iterator<Drawable<?>> it = cards.iterator();
		while (it.hasNext()) {
			Drawable<?> card = it.next();
			if (card.isAvailable()) {
				discard.add(card);
				it.remove();

				discard.remove(card);
				deck.add(card);
				i++;
			}
		}

		Utils.shuffle(deck, game.getRng());

		if (origin.synergy() == Race.DJINN) {
			manualDraw(i - 1);
		} else {
			manualDraw(i);
		}
	}

	public void verifyCap() {
		if (cards.size() > 20) {
			requestChoice(
					"str/destroy_cards",
					cards.stream().map(c -> new SelectionCard(c, false)).toList(),
					cards.size() - 20,
					ds -> {
						graveyard.addAll(ds);
						cards.removeAll(ds);

						game.reportEvent("str/discard_card", true, getName(),
								Utils.properlyJoin(game.getString("str/and")).apply(cards.stream().map(Drawable::toString).toList())
						);
					}
			);
		}
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
		cards.removeIf(d -> d.isEthereal() || !d.isAvailable());
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
		return locks.stream().filter(t -> t.obj().equals(lock)).map(Timed::time).mapToInt(AtomicInteger::get).sum();
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
			name = Utils.getOr(DAO.find(Account.class, getUid()).getName(), "???");
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

	public void modHP(double value, boolean pure) {
		if (value == 0) return;
		else if (value > 0 && origin.major() == Race.UNDEAD) return;
		else if (game.getArcade() == Arcade.OVERCHARGE) {
			value *= Math.min(0.5 + 0.5 * (Math.ceil(game.getTurn() / 2d) / 10), 1);
		} else if (game.getArcade() == Arcade.DECAY) {
			if (value > 0) {
				value /= 2;
			} else {
				value *= 1.5;
			}
		}

		if (value < 0 && origin.hasMinor(Race.BEAST)) {
			value = Math.max(-base.hp() / 4d, value);
		}

		int before = hp;

		if (!pure) {
			if (origin.hasMinor(Race.HUMAN) && value < 0) {
				value *= 1 - Math.min(game.getTurn() * 0.02, 0.75);
			}

			if (origin.synergy() == Race.POSSESSED && value > 0) {
				value *= 1 + getOther().getGraveyard().size() * 0.05;
			} else if (origin.synergy() == Race.PRIMAL && value < 0) {
				int degen = (int) (value * 0.25);
				if (degen < 0) {
					regdeg.add(degen);
					value -= degen;
				}
			}

			int dot = regdeg.peek();
			int quart = (int) (value / 4);
			if (dot > 0 && value < 0) {
				if (getOther().getOrigins().synergy() == Race.SPAWN) {
					regdeg.reduce(Degen.class, quart);
				}

				value -= quart + regdeg.reduce(Degen.class, quart);
			} else if (dot < 0 && value > 0) {
				value -= quart - regdeg.reduce(Regen.class, quart);
			}

			if (value < 0) {
				value *= Math.max(0, stats.getDamageMult().get());
				value /= (1 << getChainReduction());
			} else {
				value *= Math.max(0, stats.getHealMult().get());
			}

			double prcnt = getHPPrcnt();
			if (origin.demon()) {
				prcnt = Math.min(prcnt, 0.5);
			}

			if (this.hp + value <= 0 && prcnt > 1 / 3d) {
				if (prcnt > 2 / 3d || getGame().chance(prcnt * 100)) {
					this.hp = 1;
					return;
				}
			}

			if (origin.major() == Race.UNDEAD) {
				regdeg.add(value);
				value = 0;
			}

			Hand op = getOther();
			if (op.getOrigins().hasMinor(Race.UNDEAD) && value < 0) {
				value -= op.getGraveyard().parallelStream().mapToInt(d -> (d.getDmg() + d.getDfs()) / 100).sum();
			}
		}

		this.hp = (int) Utils.clamp(this.hp + value, 0, base.hp() * 2);

		if (!pure) {
			hpDelta = this.hp - before;
			if (hpDelta <= 0) {
				game.trigger(Trigger.ON_DAMAGE, side);

				if (origin.synergy() == Race.TORMENTED) {
					getOther().modHP((int) (hpDelta * 0.15));
				}
			} else {
				game.trigger(Trigger.ON_HEAL, side);
			}
		}

		if (origin.synergy() == Race.GARGOYLE) {
			if (isCritical()) {
				if (!reached) {
					stats.getDamageMult().set(game.getArena().DEFAULT_FIELD, -1, 1);
					reached = true;
				}
			} else {
				reached = false;
			}
		}
	}

	public boolean consumeHP(int value) {
		if (hp <= value) return false;

		this.hp = Math.max(0, this.hp - Math.max(0, value));
		return true;
	}

	public int getHpDelta() {
		return hpDelta;
	}

	public double getHPPrcnt() {
		return hp / (double) base.hp();
	}

	public boolean isLowLife() {
		return origin.demon() || getHPPrcnt() < 0.5;
	}

	public boolean isCritical() {
		return getHPPrcnt() < 0.25;
	}

	public int getMP() {
		if (origin.major() == Race.DEMON) {
			return (int) Math.max(0, hp / (base.hp() * 0.08) - 1);
		}

		return mp;
	}

	public void setMP(int mp) {
		if (origin.major() == Race.DEMON) {
			setHP((int) (Math.max(1, mp) * (base.hp() * 0.08)));
			return;
		}

		this.mp = Utils.clamp(mp, 0, 99);
	}

	public void modMP(int value) {
		if (origin.major() == Race.DEMON) {
			int unit = (int) (base.hp() * 0.08);
			int allowed = Math.max(0, hp - unit);

			value = Math.max(-allowed, value);
			modHP(value * unit);
			return;
		}

		int before = this.mp;

		this.mp = Utils.clamp(this.mp + value, 0, 99);
		mpDelta = this.mp - before;

		if (value > 0) {
			game.trigger(Trigger.ON_MP, side);

			if (origin.synergy() == Race.GHOST) {
				modHP(mpDelta * 50);
			}
		}
	}

	public boolean consumeMP(int value) {
		if (origin.major() == Race.DEMON) {
			return consumeHP((int) (value * (base.hp() * 0.08)));
		} else if (this.mp < value) return false;

		this.mp = Utils.clamp(this.mp - value, 0, 99);
		return true;
	}

	public int getMpDelta() {
		return mpDelta;
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

	public boolean canPay(Drawable<?> card) {
		return getMP() >= card.getMPCost() && getHP() >= card.getHPCost() && discard.size() >= card.getSCCost();
	}

	public boolean canPay(int mp, int hp, int sc) {
		return getMP() >= mp && getHP() >= hp && discard.size() >= sc;
	}

	public RegDeg getRegDeg() {
		return regdeg;
	}

	public void applyVoTs() {
		int val = regdeg.next();
		if (origin.synergy() == Race.DRYAD && val > 0) {
			val = 0;
		}

		modHP(val, true);
	}

	public JSONObject getData() {
		return data;
	}

	public Account getAccount() {
		if (account == null) {
			account = DAO.find(Account.class, getUid());
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

	public boolean canUseDestiny() {
		return (origin.synergy() == Race.LICH ? isLowLife() : isCritical()) && !hasUsedDestiny();
	}

	public boolean hasRerolled() {
		return Bit.on(state, 2);
	}

	public void setRerolled(boolean rerolled) {
		state = Bit.set(state, 2, rerolled);
	}

	public boolean canAttack() {
		return Bit.on(state, 3);
	}

	public void setCanAttack(boolean canAttack) {
		state = Bit.set(state, 3, canAttack);
	}

	public boolean hasSummoned() {
		return Bit.on(state, 4);
	}

	public void setSummoned(boolean summoned) {
		state = Bit.set(state, 4, summoned);
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
		return Bit.get(state, 2, 4);
	}

	public void setOriginCooldown(int time) {
		int curr = Bit.get(state, 2, 4);
		state = Bit.set(state, 2, Math.max(curr, time), 4);
	}

	public void reduceOriginCooldown(int time) {
		int curr = Bit.get(state, 2, 4);
		state = Bit.set(state, 2, Math.max(0, curr - time), 4);
	}

	public int getKills() {
		return Bit.get(state, 2, 8);
	}

	public void addKill() {
		int curr = Bit.get(state, 2, 8);
		state = Bit.set(state, 2, curr + 1, 8);
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

	public HandExtra getStats() {
		return stats;
	}

	public BufferedImage render() {
		return render(cards);
	}

	public BufferedImage render(List<Drawable<?>> cards) {
		if (cards.isEmpty()) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		BufferedImage bi = new BufferedImage((Drawable.SIZE.width + 20) * 5, (100 + Drawable.SIZE.height) * (int) Math.ceil(cards.size() / 5d), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveBold(90));

		for (int i = 0; i < cards.size(); i++) {
			int offset = bi.getWidth() / 2 - (Drawable.SIZE.width + 20) * Math.min(cards.size() - (i / 5) * 5, 5) / 2;
			int x = offset + 10 + (Drawable.SIZE.width + 10) * (i % 5);
			int y = 100 + (100 + Drawable.SIZE.height) * (i / 5);

			Drawable<?> d = cards.get(i);
			boolean ally = equals(d.getHand());

			if (getLockTime(Lock.BLIND) > 0 && ally) {
				g2d.drawImage(userDeck.getStyling().getFrame().getBack(userDeck), x, y, null);
			} else {
				g2d.drawImage(d.render(game.getLocale(), userDeck), x + 15, y + 15, null);

				if (!ally) {
					Graph.applyTransformed(g2d, x + 15, y + 15, g -> {
						g.setClip(userDeck.getStyling().getFrame().getBoundary());
						g.drawImage(IO.getResourceAsImage("shoukan/states/sight.png"), 0, 0, null);
					});
				}

				if (d instanceof EffectHolder<?> e && e.hasFlag(Flag.EMPOWERED)) {
					boolean legacy = userDeck.getStyling().getFrame().isLegacy();
					BufferedImage emp = IO.getResourceAsImage("shoukan/frames/state/" + (legacy ? "old" : "new") + "/empowered.png");

					g2d.drawImage(emp, x + 15, y + 15, null);
				}
			}

			if (d.isAvailable() && ally) {
				Graph.drawOutlinedString(g2d, String.valueOf(i + 1), x + (Drawable.SIZE.width / 2 - g2d.getFontMetrics().stringWidth(String.valueOf(i + 1)) / 2), y - 10, 6, Color.BLACK);
			}
		}

		g2d.dispose();

		return bi;
	}

	public void send(BufferedImage bi) {
		if (bi == null) return;

		getUser().openPrivateChannel().flatMap(chn -> chn.sendFiles(FileUpload.fromData(IO.getBytes(bi, "png"), "hand.png"))).queue(m -> {
			if (lastMessage != null) {
				m.getChannel().retrieveMessageById(lastMessage).flatMap(Objects::nonNull, Message::delete).queue(null, Utils::doNothing);
			}

			lastMessage = m.getId();
		}, Utils::doNothing);
	}

	public void showHand() {
		showHand(this);
	}

	public void showHand(Hand hand) {
		send(render(hand.getCards()));
	}

	public void showCards(List<Drawable<?>> cards) {
		send(render(cards));
	}

	public void requestChoice(List<SelectionCard> cards, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		requestChoice("str/select_a_card", cards, 1, action);
	}

	public void requestChoice(String caption, List<SelectionCard> cards, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		requestChoice(caption, cards, 1, action);
	}

	public void requestChoice(List<SelectionCard> cards, int required, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		requestChoice("str/select_a_card", cards, required, action);
	}

	public void requestChoice(String caption, List<SelectionCard> cards, int required, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		if (selection != null) {
			throw new SelectionException("err/pending_selection");
		}

		if (cards.isEmpty()) throw new ActivationException("err/empty_selection");
		else if (cards.size() < required) throw new ActivationException("err/insufficient_selection");

		selection = new SelectionAction(
				caption, cards, required,
				new ArrayList<>(),
				new SupplyChain<List<Drawable<?>>>(null)
						.add(cs -> {
							selection = null;
							System.out.println("clear");

							return cs;
						})
						.add(cs -> {
							System.out.println("run");
							action.accept(cs);
							return cs;
						})
		);

		showChoices();
		game.getChannel().sendMessage(game.getString("str/selection_sent")).queue();
	}

	public BufferedImage renderChoices() {
		List<SelectionCard> cards = selection.cards();
		if (cards.isEmpty()) return null;

		BufferedImage bi = new BufferedImage((Drawable.SIZE.width + 20) * 5, 100 + (100 + Drawable.SIZE.height) * (int) Math.ceil(cards.size() / 5d), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveBold(60));
		g2d.translate(0, 100);

		String str = game.getString(selection.caption(), selection.required());
		Graph.drawOutlinedString(g2d, str, bi.getWidth() / 2 - g2d.getFontMetrics().stringWidth(str) / 2, -10, 6, Color.BLACK);

		for (int i = 0; i < cards.size(); i++) {
			int offset = bi.getWidth() / 2 - (Drawable.SIZE.width + 20) * Math.min(cards.size() - (i / 5) * 5, 5) / 2;
			int x = offset + 10 + (Drawable.SIZE.width + 10) * (i % 5);
			int y = 100 + (100 + Drawable.SIZE.height) * (i / 5);

			SelectionCard d = cards.get(i);
			Deck deck = Utils.getOr(d.card().getHand(), this).userDeck;
			BufferedImage img;

			if (d.hidden()) {
				img = deck.getStyling().getFrame().getBack(deck);
			} else {
				img = d.card().render(game.getLocale(), deck);
			}

			if (selection.indexes().contains(i)) {
				RescaleOp op = new RescaleOp(0.5f, 0, null);
				op.filter(img, img);

				Graphics2D g = img.createGraphics();
				g.drawImage(IO.getResourceAsImage("shoukan/states/selected.png"), 0, 0, null);
				g.dispose();
			}

			Graph.drawOutlinedString(g2d, String.valueOf(i + 1),
					x + (Drawable.SIZE.width / 2 - g2d.getFontMetrics().stringWidth(String.valueOf(i + 1)) / 2), y - 10,
					6, Color.BLACK
			);

			g2d.drawImage(img, x, y, null);
		}

		g2d.dispose();

		return bi;
	}

	public void showChoices() {
		send(renderChoices());
	}

	public SelectionAction getSelection() {
		return selection;
	}

	public boolean selectionPending() {
		return selection != null;
	}

	public boolean actionPrevented() {
		return preventAction;
	}

	public void preventAction() {
		preventAction = true;
	}

	public void allowAction() {
		preventAction = false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Hand hand = (Hand) o;
		return SERIAL == hand.SERIAL && game == hand.game && side == hand.side;
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, game, side);
	}
}
