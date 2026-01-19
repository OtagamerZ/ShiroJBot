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
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.interfaces.shoukan.Proxy;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.SupplyChain;
import com.kuuhaku.model.common.TimedMap;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.SelectionAction;
import com.kuuhaku.model.records.SelectionCard;
import com.kuuhaku.model.records.shoukan.*;
import com.kuuhaku.util.Bit32;
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
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;

public class Hand {
	private final long SERIAL = ThreadLocalRandom.current().nextLong();
	private final BondedList<Drawable<?>> fakeDeck = new BondedList<>((a, b) -> false);

	private final Shoukan game;
	private final Deck userDeck;
	private final Side side;
	private final HandExtra stats = new HandExtra();
	private final BondedList<Drawable<?>> cards = new BondedList<>((d, it) -> {
		if (getCards(false).contains(d)) return false;
		else if (d instanceof EffectHolder<?> eh) {
			if (d instanceof Proxy<?> p && !p.hasOwnEffect()) {
				d.reset();
				it.add(p.getOriginal());
				return false;
			}

			if (eh.hasFlag(Flag.NO_CONVERT, true) && d.getHand() != null && !equals(d.getHand())) return false;
		}

		d.setHand(this);
		getGame().trigger(Trigger.ON_HAND, d.asSource(Trigger.ON_HAND));

		if (getOrigins().isPure(Race.DIVINITY) && d.isEthereal()) {
			int eths = getData().getInt("eth_drawn") + 1;
			if (eths % 2 == 0) {
				modMP(1);
			}

			getData().put("eth_drawn", eths);
		}

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			for (Evogear evogear : s.getEquipments()) {
				it.add(evogear);
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.setSlot(null);

		if (d instanceof Proxy<?> p) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		if (d instanceof EffectHolder<?> eh && eh.hasFlag(Flag.DESTROY)) {
			if (d.getCurrentStack() != null) {
				d.getCurrentStack().remove(d);
			}

			return false;
		}

		return true;
	}, d -> d.setCurrentStack(getCards(false)), Utils::doNothing);
	private final BondedList<Drawable<?>> discard = new BondedList<>((d, it) -> {
		if (getDiscard(false).contains(d)) return false;
		else if (d instanceof EffectHolder<?> eh) {
			if (d instanceof Proxy<?> p && !p.hasOwnEffect()) {
				d.reset();
				it.add(p.getOriginal());
				return false;
			}

			if (eh.hasFlag(Flag.NO_CONVERT, true) && d.getHand() != null && !equals(d.getHand())) return false;
		}

		d.setHand(this);
		getData().put("last_discarded", d);
		getGame().trigger(Trigger.ON_DISCARD, d.asSource(Trigger.ON_DISCARD));

		if (d instanceof Proxy<?> p) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		if (d instanceof EffectHolder<?> eh && eh.hasFlag(Flag.DESTROY)) {
			if (d.getCurrentStack() != null) {
				d.getCurrentStack().remove(d);
			}

			return false;
		}

		return true;
	}, d -> d.setCurrentStack(getDiscard(false)), Utils::doNothing);
	private final BondedList<Drawable<?>> deck = new BondedList<>((d, it) -> {
		if (getLockTime(Lock.DECK) > 0) return false;
		else if (getRealDeck(false).contains(d)) return false;
		else if (d instanceof EffectHolder<?> eh) {
			if (d instanceof Proxy<?> p && !p.hasOwnEffect()) {
				d.reset();
				it.add(p.getOriginal());
				return false;
			}

			if (eh.hasFlag(Flag.NO_CONVERT, true) && d.getHand() != null && !equals(d.getHand())) return false;
		}

		d.setHand(this);
		getGame().trigger(Trigger.ON_DECK, d.asSource(Trigger.ON_DECK));

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			for (Evogear evogear : s.getEquipments()) {
				it.add(evogear);
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();

		if (d instanceof Proxy<?> p) {
			it.add(p.getOriginal());
			return false;
		}

		if (d.isEthereal() || (d instanceof EffectHolder<?> eh && eh.hasFlag(Flag.DESTROY))) {
			if (d.getCurrentStack() != null) {
				d.getCurrentStack().remove(d);
			}

			return false;
		}

		return true;
	}, d -> d.setCurrentStack(getRealDeck(false)), Utils::doNothing);
	private final BondedList<Drawable<?>> graveyard = new BondedList<>((d, it) -> {
		if (getGraveyard(false).contains(d)) return false;
		else if (d instanceof EffectHolder<?> eh) {
			if (d instanceof Proxy<?> p && !p.hasOwnEffect()) {
				d.reset();
				it.add(p.getOriginal());
				return false;
			}

			if (eh.hasFlag(Flag.NO_CONVERT, true) && d.getHand() != null && !equals(d.getHand())) return false;
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
				getGame().trigger(Trigger.ON_CONFIRMED_KILL, s.getLastInteraction().asSource(Trigger.ON_CONFIRMED_KILL), s.asTarget());

				if (op.getOrigins().hasMinor(Race.UNDEAD)) {
					getRegDeg().add((s.getDmg() + s.getDfs()) / 10);
				}

				if (op.getOrigins().hasSynergy(Race.SHINIGAMI)) {
					op.getCards().add(s.withCopy(c -> {
						c.setEthereal(true);
						c.getStats().getCost().set(new MultMod(0.5));
					}));
				}
				if (op.getOrigins().hasSynergy(Race.REAPER)) {
					op.getDiscard().add(d.copy());
				}

				if (getOrigins().hasSynergy(Race.ESPER)) {
					getCards().add(s.withCopy(c -> c.setEthereal(true)));
				}
			}

			if (!s.getEquipments().isEmpty()) {
				for (Evogear evogear : s.getEquipments()) {
					it.add(evogear);
				}
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();

		if (d instanceof Proxy<?> p) {
			it.add(p.getOriginal());
			return false;
		}

		if (d.isEthereal() || (d instanceof EffectHolder<?> eh && eh.hasFlag(Flag.DESTROY))) {
			if (d.getCurrentStack() != null) {
				d.getCurrentStack().remove(d);
			}

			return false;
		}

		return true;
	}, d -> d.setCurrentStack(getGraveyard(false)), Utils::doNothing);
	private final TimedMap<Lock> locks = new TimedMap<>();

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
	private transient SpecialDefeat defeat;
	private transient int hpDelta = 0;
	private transient int mpDelta = 0;
	private transient boolean preventAction = false;
	private transient boolean reached = false;

	@Transient
	private int state = 0b100;
	/*
	0xFFFF 0 F FF
	  └┤└┤   │ └┴ 0001 1111
	   │ │   │       │ │││└ forfeit
	   │ │   │       │ ││└─ destiny
	   │ │   │       │ │└── reroll
	   │ │   │       │ └─── can attack
	   │ │   │       └ has summoned
	   │ │   └ (0 - 15) cooldown
	   │ │
	   │ └─── (0 - 255) kills
	   └───── (0 - 255) cards spent
	 */

	private final transient Queue<SelectionAction> selection = new ArrayDeque<>();

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
		this.userDeck = DAO.find(Account.class, uid).getDeck();
		if (userDeck == null) {
			throw new GameReport(GameReport.NO_DECK, uid);
		}

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
	}

	public void loadCards() {
		List<? extends Drawable<?>> toAdd;
		if (game.getArcade() == Arcade.CARDMASTER) {
			List<List<? extends Drawable<?>>> cards = new ArrayList<>();
			cards.add(DAO.queryAll(Senshi.class, "SELECT s FROM Senshi s WHERE cast(get_rarity_index(s.card.rarity) AS INTEGER) BETWEEN 1 AND 5 AND NOT CAST(has(s.base.tags, 'FUSION') AS BOOLEAN)"));
			cards.add(DAO.queryAll(Evogear.class, "SELECT e FROM Evogear e WHERE e.base.mana > 0"));
			cards.add(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE NOT f.effectOnly"));

			toAdd = cards.parallelStream()
					.flatMap(List::stream)
					.map(Drawable::copy)
					.toList();
		} else {
			toAdd = Stream.of(userDeck.getSenshiRaw(), userDeck.getEvogearRaw(), userDeck.getFieldsRaw())
					.parallel()
					.flatMap(List::stream)
					.map(DeckEntry::card)
					.toList();
		}

		BondedList<Drawable<?>> stack = getMainStack();
		stack.addAll(toAdd);

		Hero h = userDeck.getHero(game.getLocale());
		if (h != null) {
			Senshi hero = h.createSenshi();
			hero.getStats().getPower().set(new MultMod(h.getAttributes().wis() * 0.05));

			stack.add(hero);
		}

		Utils.shuffle(stack, game.getRng());
		if (getGame().getArcade() != Arcade.CARDMASTER) {
			List<Evogear> strats = stack.stream()
					.filter(d -> d instanceof Evogear e && e.getTags().contains("STRATAGEM"))
					.map(d -> (Evogear) d)
					.toList();

			stack.removeAll(strats);
			for (Evogear e : strats) {
				e.executeAssert(ON_INITIALIZE);
				getGame().getChannel().buffer(getGame().getString("str/stratagem_use", e));
			}
		}
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
		if (game == null) return this;
		return game.getHands().get(side.getOther());
	}

	public Origin getOrigins() {
		return origin;
	}

	public void setOrigin(Origin origin) {
		this.origin = origin;
	}

	public BondedList<Drawable<?>> getCards() {
		return getCards(true);
	}

	public BondedList<Drawable<?>> getCards(boolean sweep) {
		if (sweep) {
			cards.removeIf(d -> d.checkRemoval(this, true, cards));
		}

		return cards;
	}

	public BondedList<Drawable<?>> getRealDeck() {
		return getRealDeck(true);
	}

	public BondedList<Drawable<?>> getRealDeck(boolean sweep) {
		if (sweep) {
			deck.removeIf(d -> d.checkRemoval(this, false, deck));
		}

		return deck;
	}

	public BondedList<Drawable<?>> getMainStack() {
		return origin.hasSynergy(Race.LICH) ? graveyard : deck;
	}

	public BondedList<Drawable<?>> getDeck() {
		if (getLockTime(Lock.DECK) > 0) {
			return fakeDeck;
		}

		return getRealDeck();
	}

	public Drawable<?> manualDraw() {
		return manualDraw(true);
	}

	public List<Drawable<?>> manualDraw(int value) {
		if (value <= 0) return List.of();

		List<Drawable<?>> out = new ArrayList<>();
		for (int i = 0; i < Math.min(value, getMainStack().size()); i++) {
			Drawable<?> d = manualDraw(false);
			if (d != null) {
				out.add(d);
			}
		}

		data.put("last_drawn_batch", List.copyOf(out));
		game.trigger(ON_DRAW_MULTIPLE, getSide());
		return out;
	}

	public Drawable<?> manualDraw(boolean trigger) {
		BondedList<Drawable<?>> stack = getMainStack();

		Drawable<?> out;
		if (origin.hasSynergy(Race.GEIST)) {
			out = stack.removeFirst(d -> !(d instanceof Senshi));
		} else {
			if (cards.stream().noneMatch(d -> d instanceof Senshi)) {
				out = stack.removeFirst(d -> d instanceof Senshi);
			} else {
				out = stack.removeFirst();
			}
		}

		if (out != null) {
			try {
				return addToHand(out, true);
			} finally {
				if (trigger) {
					game.trigger(ON_DRAW_SINGLE, getSide());
				}
			}
		}

		return null;
	}

	public Drawable<?> draw() {
		return draw(true);
	}

	public Drawable<?> draw(boolean trigger) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return null;

		BondedList<Drawable<?>> deck = getDeck();
		Drawable<?> out = deck.removeFirst();
		if (out != null) {
			try {
				return addToHand(out, false);
			} finally {
				if (trigger) {
					game.trigger(ON_DRAW_SINGLE, getSide());
				}
			}
		}

		return null;
	}

	public List<Drawable<?>> draw(int value) {
		if (game.getArcade() == Arcade.DECK_ROYALE) return List.of();

		List<Drawable<?>> out = new ArrayList<>();
		for (int i = 0; i < value; i++) {
			Drawable<?> d = draw(false);
			if (d == null) return out;

			out.add(d);
		}

		data.put("last_drawn_batch", List.copyOf(out));
		game.trigger(ON_DRAW_MULTIPLE, getSide());
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
		Drawable<?> out = deck.removeFirst(d -> d instanceof Senshi s && s.getRace().isRace(race));
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
				if (origin.hasSynergy(Race.MUMMY) && out.getCurses().isEmpty() && !selectionPending()) {
					Evogear curse = Evogear.getByTag(game.getRng(), "MUMMY_CURSE").getRandom();

					requestChoice(
							null,
							List.of(
									new SelectionCard(out, false),
									new SelectionCard(out.withCopy(c -> {
										((Evogear) c).getStats().getAttr().set(new MultMod(0.3));
										c.getCurses().add(curse.getEffect());
									}), false)
							),
							ds -> addToHand(ds.getFirst(), false, true)
					);

					return null;
				}
			}

			if (getOther().getOrigins().hasSynergy(Race.IMP)) {
				modHP(-75);
			}
		}

		out.setAvailable(true);
		cards.add(out);

		if (manual) {
			consumeDraw();
		}

		data.put("last_drawn", out);
		data.put("last_drawn_" + out.getClass().getSimpleName().toLowerCase(), out);

		game.trigger(Trigger.ON_DRAW, side);
		if (manual) {
			game.trigger(Trigger.ON_MANUAL_DRAW, out.asSource(Trigger.ON_MANUAL_DRAW));
		} else {
			game.trigger(Trigger.ON_MAGIC_DRAW, out.asSource(Trigger.ON_MAGIC_DRAW));
		}

		return out;
	}

	public void rerollHand() {
		if (getLockTime(Lock.DECK) > 0) return;

		var reroll = cards.stream()
				.filter(Drawable::isAvailable)
				.collect(Collectors.toCollection(ArrayList::new));

		if (reroll.isEmpty()) return;
		if (origin.hasSynergy(Race.DJINN)) {
			Drawable<?> offer = Utils.getRandomEntry(reroll);
			reroll.remove(offer);
			cards.remove(offer);

			getOther().cards.add(offer);
		}

		discard.addAll(reroll);
		deck.addAll(reroll);
		cards.removeAll(reroll);

		Utils.shuffle(deck, game.getRng());

		int draws = reroll.size();
		consumeDraws(-draws);
		manualDraw(draws);
	}

	public void verifyCap() {
		List<SelectionCard> cards = this.cards.stream()
				.filter(Drawable::isAvailable)
				.map(c -> new SelectionCard(c, false))
				.toList();

		if (cards.size() > 20) {
			requestDiscard(cards.size() - 20);
		}
	}

	public CompletableFuture<List<Drawable<?>>> requestDiscard(int amount) {
		List<SelectionCard> cards = this.cards.stream()
				.filter(Drawable::isAvailable)
				.map(c -> new SelectionCard(c, false))
				.toList();

		return requestChoice(null, "str/destroy_cards", cards, new SelectionRange(amount),
				ds -> {
					graveyard.addAll(ds);
					this.cards.removeAll(ds);

					game.reportEvent("str/discarded_card", true, false, getName(),
							Utils.properlyJoin(game.getLocale(), ds.stream().map(Drawable::toString).toList())
					);
				}
		);
	}

	public BondedList<Drawable<?>> getGraveyard() {
		return getGraveyard(true);
	}

	public BondedList<Drawable<?>> getGraveyard(boolean sweep) {
		if (sweep) {
			graveyard.removeIf(d -> d.checkRemoval(this, false, graveyard));
		}

		return graveyard;
	}

	public BondedList<Drawable<?>> getDiscard() {
		return getDiscard(true);
	}

	public BondedList<Drawable<?>> getDiscard(boolean sweep) {
		if (sweep) {
			discard.removeIf(d -> d.checkRemoval(this, false, discard));
		}

		return discard;
	}

	public void flushDiscard() {
		cards.removeIf(d -> d.isEthereal() || !d.isAvailable());
		graveyard.addAll(discard);

		if (origin.hasSynergy(Race.REAPER)) {
			regdeg.add(100 * discard.size());
		}

		discard.clear();
	}

	public TimedMap<Lock> getLocks() {
		return locks;
	}

	public void modLockTime(Lock lock, int time) {
		if (time == 0) return;
		locks.add(lock, time);
	}

	public int getLockTime(Lock lock) {
		return locks.getTime(lock);
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

	public void setName(String name) {
		this.name = name;
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

		if (value > 0 && origin.major() == Race.UNDEAD) return;
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
			value = Math.max(-base.hp() / 3d, value);
		}

		int before = hp;

		if (!pure) {
			if (value > 0) {
				if (origin.major() == Race.HUMAN) {
					value *= 1.25;
				}

				if (origin.hasSynergy(Race.POSSESSED)) {
					value *= 1 + getOther().getGraveyard().size() * 0.05;
				}
			} else if (value < 0) {
				if (origin.major() == Race.HUMAN && isCritical()) {
					value /= 2;
				}

				if (origin.hasMinor(Race.HUMAN)) {
					value *= 1 - Math.min(game.getTurn() * 0.02, 0.75);
				}
			}

			if (getOther().getOrigins().synergy() != Race.SPAWN) {
				int dot = regdeg.peek();
				int half = (int) (value / 4);
				if (dot > 0 && value < 0) {
					value -= half + regdeg.reduce(Degen.class, half);
				}
			}

			value = (value < 0 ? stats.getDamageMult() : stats.getHealMult()).apply(value);

			if (value < 0 && origin.major() == Race.UNDEAD) {
				regdeg.add(value);
				value = 0;
			}
		}

		int min = 0;
		double prcnt = getHPPrcnt();
		if (this.hp + value <= 0 && prcnt > 1 / 3d) {
			if (!origin.demon() && (prcnt > 2 / 3d || game.chance(prcnt * 100))) {
				min = 1;
			}
		}

		this.hp = (int) Utils.clamp(this.hp + value, min, base.hp() * 2);

		hpDelta = this.hp - before;
		if (hpDelta <= 0) {
			game.trigger(Trigger.ON_DAMAGE, side);

			if (origin.hasSynergy(Race.TORMENTED)) {
				getOther().modHP((int) (hpDelta * 0.15));
			}
		} else {
			game.trigger(Trigger.ON_HEAL, side);
		}

		if (origin.hasSynergy(Race.GARGOYLE)) {
			if (isCritical()) {
				if (!reached) {
					stats.getDamageMult().set(new MultMod(game.getArena().DEFAULT_FIELD, -1, 1));
					reached = true;
				}
			} else {
				reached = false;
			}
		}
	}

	public boolean consumeHP(int value) {
		return consumeHP(value, false);
	}

	public boolean consumeHP(int value, boolean force) {
		if (!force && this.hp <= value) return false;

		int before = this.hp;
		this.hp = Math.max(1, this.hp - Math.max(0, value));
		hpDelta = this.hp - before;

		if (origin.isPure(Race.DEMON)) {
			regdeg.add(-hpDelta);
		}

		if (value > 0) {
			game.trigger(Trigger.ON_CONSUME_HP, side);
		}

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

			if (origin.hasSynergy(Race.GHOST)) {
				modHP(mpDelta * 50);
			}
		}
	}

	public boolean consumeMP(int value) {
		if (origin.major() == Race.DEMON) {
			int val = (int) (value * (base.hp() * 0.08));
			if (origin.isPure()) {
				regdeg.add(val, 0);
			}

			return consumeHP(val);
		} else if (this.mp < value) return false;

		int before = this.mp;
		this.mp = Utils.clamp(this.mp - value, 0, 99);
		mpDelta = this.mp - before;

		if (value > 0) {
			game.trigger(Trigger.ON_CONSUME_MP, side);
		}

		return true;
	}

	public int getMpDelta() {
		return mpDelta;
	}

	public List<Drawable<?>> consumeSC(int value) {
		List<Drawable<?>> consumed = new ArrayList<>();

		for (int i = 0; i < value && !discard.isEmpty(); i++) {
			Drawable<?> card = discard.removeLast();

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

	public void applyRegDeg() {
		int val = regdeg.next();
		if (val == 0) return;

		if (origin.hasSynergy(Race.DRYAD) && val > 0) {
			val = 0;
		}

		modHP(val, true);
		game.trigger(val < 0 ? Trigger.ON_DEGEN : Trigger.ON_REGEN, getSide());

		if (val < 0 && getOther().getOrigins().hasSynergy(Race.VIRUS)) {
			getOther().modHP(-val / 2);
		}
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
		return Bit32.on(state, 0);
	}

	public void setForfeit(boolean forfeit) {
		state = Bit32.set(state, 0, forfeit);
	}

	public boolean hasUsedDestiny() {
		return Bit32.on(state, 1);
	}

	public void setUsedDestiny(boolean usedDestiny) {
		state = Bit32.set(state, 1, usedDestiny);
	}

	public boolean canUseDestiny() {
		return isCritical() && !hasUsedDestiny();
	}

	public boolean hasRerolled() {
		return Bit32.on(state, 2);
	}

	public void setRerolled(boolean rerolled) {
		state = Bit32.set(state, 2, rerolled);
	}

	public boolean canAttack() {
		return Bit32.on(state, 3);
	}

	public void setCanAttack(boolean canAttack) {
		state = Bit32.set(state, 3, canAttack);
	}

	public boolean hasSummoned() {
		return Bit32.on(state, 4);
	}

	public void setSummoned(boolean summoned) {
		state = Bit32.set(state, 4, summoned);
	}

	public boolean isDefeated() {
		return defeat != null;
	}

	public SpecialDefeat getDefeat() {
		return defeat;
	}

	public void setDefeat(SpecialDefeat defeat) {
		this.defeat = defeat;
	}

	public int getOriginCooldown() {
		return Bit32.get(state, 2, 4);
	}

	public void setOriginCooldown(int time) {
		int curr = Bit32.get(state, 2, 4);
		state = Bit32.set(state, 2, Math.max(curr, time), 4);
	}

	public void reduceOriginCooldown(int time) {
		int curr = Bit32.get(state, 2, 4);
		state = Bit32.set(state, 2, Math.max(0, curr - time), 4);
	}

	public int getKills() {
		return Bit32.get(state, 2, 8);
	}

	public void addKill() {
		int curr = Bit32.get(state, 2, 8);
		state = Bit32.set(state, 2, curr + 1, 8);
	}

	public int getRemainingDraws() {
		return Bit32.get(state, 3, 8);
	}

	public void consumeDraw() {
		consumeDraws(1);
	}

	public void consumeDraws(int draws) {
		int curr = Bit32.get(state, 3, 8);
		state = Bit32.set(state, 3, curr - draws, 8);
	}

	public void resetDraws() {
		state = Bit32.set(state, 3, Math.max(0, base.handCapacity().get() - cards.size()), 8);
	}

	public void flushStacks() {
		Stream.of(cards, graveyard, deck, discard)
				.parallel()
				.forEach(s -> s.removeIf(d -> d.getCurrentStack() != s));
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
				g2d.drawImage(userDeck.getFrame().getBack(userDeck), x, y, null);
			} else {
				g2d.drawImage(d.render(game.getLocale(), userDeck), x, y, null);

				if (!ally) {
					Graph.applyTransformed(g2d, x, y, g -> {
						g.setClip(userDeck.getFrame().getBoundary());
						g.drawImage(IO.getResourceAsImage("shoukan/states/sight.png"), 15, 15, null);
					});
				}

				if (d instanceof EffectHolder<?> e && e.hasFlag(Flag.EMPOWERED)) {
					boolean legacy = userDeck.getFrame().isLegacy();
					BufferedImage emp = IO.getResourceAsImage("shoukan/frames/state/" + (legacy ? "old" : "new") + "/empowered.png");

					g2d.drawImage(emp, x, y, null);
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

		User user = getUser();
		if (user == null) return;

		user.openPrivateChannel().flatMap(chn -> chn.sendFiles(FileUpload.fromData(IO.getBytes(bi, "png"), "hand.png"))).queue(m -> {
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

	public CompletableFuture<List<Drawable<?>>> requestChoice(Drawable<?> source, List<SelectionCard> cards, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		return requestChoice(source, SelectionRange.SINGLE.label(), cards, SelectionRange.SINGLE, action);
	}

	public CompletableFuture<List<Drawable<?>>> requestChoice(Drawable<?> source, String caption, List<SelectionCard> cards, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		return requestChoice(source, caption, cards, SelectionRange.SINGLE, action);
	}

	public CompletableFuture<List<Drawable<?>>> requestChoice(Drawable<?> source, List<SelectionCard> cards, SelectionRange range, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		return requestChoice(source, range.label(), cards, range, action);
	}

	public CompletableFuture<List<Drawable<?>>> requestChoice(Drawable<?> source, String caption, List<SelectionCard> cards, SelectionRange range, ThrowingConsumer<List<? extends Drawable<?>>> action) {
		if (cards.isEmpty()) throw new ActivationException("err/empty_selection");
		else if (cards.size() < range.min()) {
			throw new ActivationException("err/insufficient_selection");
		}

		CompletableFuture<List<Drawable<?>>> task = new CompletableFuture<>();
		selection.add(new SelectionAction(
				source, caption, cards, range,
				new ArrayList<>(),
				new SupplyChain<List<Drawable<?>>>(null)
						.add(cs -> {
							selection.poll();
							return cs;
						})
						.add(cs -> {
							action.accept(cs);
							task.complete(cs);
							return cs;
						})
		));

		game.getChannel().sendMessage(game.getString("str/selection_sent")).queue();
		return task;
	}

	public BufferedImage renderChoices() {
		if (selection.isEmpty()) return null;

		SelectionAction sel = selection.peek();
		List<SelectionCard> cards = sel.cards();
		if (cards.isEmpty()) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		BufferedImage bi = new BufferedImage((Drawable.SIZE.width + 20) * 5, 100 + (100 + Drawable.SIZE.height) * (int) Math.ceil(cards.size() / 5d), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveBold(60));
		g2d.translate(0, 100);

		String str = game.getString(sel.caption(), sel.range().min(), sel.range().max());
		Graph.drawOutlinedString(g2d, str, bi.getWidth() / 2 - g2d.getFontMetrics().stringWidth(str) / 2, -10, 6, Color.BLACK);

		for (int i = 0; i < cards.size(); i++) {
			int offset = bi.getWidth() / 2 - (Drawable.SIZE.width + 20) * Math.min(cards.size() - (i / 5) * 5, 5) / 2;
			int x = offset + 10 + (Drawable.SIZE.width + 10) * (i % 5);
			int y = 100 + (100 + Drawable.SIZE.height) * (i / 5);

			SelectionCard d = cards.get(i);
			Deck deck = Utils.getOr(d.card().getHand(), this).userDeck;
			BufferedImage img;

			if (d.hidden()) {
				img = deck.getFrame().getBack(deck);
			} else {
				img = d.card().render(game.getLocale(), deck);
			}

			if (sel.indexes().contains(i)) {
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

	public SelectionAction getSelection() {
		return selection.peek();
	}

	public boolean selectionPending() {
		return !selection.isEmpty();
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
