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

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedLinkedList;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.model.records.shoukan.Timed;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Hand {
	private final long SERIAL = Constants.DEFAULT_RNG.nextLong();

	private final String uid;
	private final Shoukan game;
	private final Deck userDeck;

	private final Side side;
	private final Origin origin;

	private final List<Drawable<?>> cards = new BondedList<>(Objects::nonNull, d -> {
		d.setHand(this);
		getGame().trigger(Trigger.ON_HAND, d.asSource(Trigger.ON_HAND));

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			getCards().addAll(s.getEquipments());
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();
	});
	private final LinkedList<Drawable<?>> deck = new BondedLinkedList<>(Objects::nonNull, d -> {
		d.setHand(this);
		getGame().trigger(Trigger.ON_DECK, d.asSource(Trigger.ON_DECK));

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			getDeck().addAll(s.getEquipments());
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();
	});
	private final LinkedList<Drawable<?>> graveyard = new BondedLinkedList<>(
			d -> d != null && !(d instanceof Senshi s && s.getStats().popFlag(Flag.NO_DEATH)) && !d.isSPSummon(),
			d -> {
				getGame().trigger(Trigger.ON_GRAVEYARD, d.asSource(Trigger.ON_GRAVEYARD));

				if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
					getGraveyard().addAll(s.getEquipments());
				} else if (d instanceof Evogear e && e.getEquipper() != null) {
					e.getEquipper().getEquipments().remove(e);
				}

				d.reset();

				if (d.isSolid() && d.getHand().getOrigin().synergy() == Race.REBORN && Calc.chance(5)) {
					cards.add(d.copy());
					d.setSolid(false);
				}

				getGraveyard().removeIf(dr -> !dr.isSolid());
			}
	);
	private final List<Drawable<?>> discard = new BondedList<>(Objects::nonNull, d -> {
		getGame().trigger(Trigger.ON_DISCARD, d.asSource(Trigger.ON_DISCARD));
		d.setAvailable(false);
	});
	private final Set<Timed<Lock>> locks = new HashSet<>();
	private final Set<EffectHolder<?>> leeches = new HashSet<>();

	private final BaseValues base;
	private final RegDeg regdeg = new RegDeg();
	private final JSONObject data = new JSONObject();

	private String name;

	private int hp;
	private int mp;

	private transient Account account;
	private transient String lastMessage;
	private transient boolean forfeit;
	private transient int kills = 0;
	private transient int cooldown = 0;
	/*
	0x0000 FF FF
	       └┤ └┤
	        │  └ (0 - 255) minor effect
	        └─ (0 - 255) major effect
	 */

	public Hand(String uid, Shoukan game, Side side) {
		this.uid = uid;
		this.game = game;
		this.userDeck = DAO.find(Account.class, uid).getCurrentDeck();
		if (userDeck == null) {
			throw new GameReport(GameReport.NO_DECK, uid);
		} else if (!(userDeck.validateSenshi() && userDeck.validateEvogear() && userDeck.validateFields())) {
			throw new GameReport(GameReport.INVALID_DECK, uid);
		}

		this.side = side;
		this.origin = userDeck.getOrigins();
		this.base = userDeck.getBaseValues(this);
		this.hp = base.hp();

		deck.addAll(
				Stream.of(userDeck.getSenshi(), userDeck.getEvogear(), userDeck.getFields())
						.parallel()
						.flatMap(List::stream)
						.map(d -> d.copy())
						.peek(d -> {
							if (d instanceof Field f && origin.synergy() == Race.PIXIE) {
								Utils.shufflePairs(f.getModifiers());
							}

							d.setSolid(true);
						})
						.collect(Utils.toShuffledList(Constants.DEFAULT_RNG))
		);
		// TODO Secondary divinity
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

	public Origin getOrigin() {
		return origin;
	}

	public List<Drawable<?>> getCards() {
		cards.removeIf(d -> !d.getHand().equals(this));

		return cards;
	}

	public int getHandCount() {
		return (int) cards.stream().filter(Drawable::isSolid).count();
	}

	public int getRemainingDraws() {
		return Math.max(0, base.handCapacity().apply(game.getTurn()) - getHandCount());
	}

	public LinkedList<Drawable<?>> getRealDeck() {
		deck.removeIf(d -> !d.getHand().equals(this));

		return deck;
	}

	public LinkedList<Drawable<?>> getDeck() {
		if (getLockTime(Lock.DECK) > 0) {
			return new LinkedList<>();
		}

		return getRealDeck();
	}

	public void manualDraw(int value) {
		if (deck.isEmpty()) return;

		if (cards.stream().noneMatch(d -> d instanceof Senshi)) {
			for (int i = 0; i < deck.size() && value > 0; i++) {
				if (deck.get(i) instanceof Senshi) {
					if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
						modHP(-10);
					}

					cards.add(deck.remove(i));
					getGame().trigger(Trigger.ON_DRAW);
					value--;
					break;
				}
			}
		}

		for (int i = 0; i < value; i++) {
			Drawable<?> d = deck.pollFirst();

			if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
				modHP(50);
			}
			if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
				modHP(-10);
			}

			if (d != null) {
				cards.add(d);
				getGame().trigger(Trigger.ON_DRAW);
			}
		}
	}

	public Drawable<?> draw() {
		LinkedList<Drawable<?>> deck = getDeck();
		Drawable<?> d = deck.pollFirst();

		if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
			modHP(50);
		}
		if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
			modHP(-10);
		}

		if (d != null) {
			cards.add(d);
			getGame().trigger(Trigger.ON_DRAW);
		}

		return d;
	}

	public List<Drawable<?>> draw(int value) {
		List<Drawable<?>> out = new ArrayList<>();
		for (int i = 0; i < value; i++) {
			Drawable<?> d = draw();
			if (d != null) {
				out.add(d);
			}
		}
		return out;
	}

	public Drawable<?> draw(String card) {
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			Drawable<?> d = deck.get(i);
			if (d.getCard().getId().equalsIgnoreCase(card)) {
				if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
					modHP(50);
				}
				if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
					modHP(-10);
				}

				Drawable<?> out = deck.remove(i);
				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> draw(Race race) {
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Senshi s && s.getRace().isRace(race)) {
				if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
					modHP(-10);
				}

				Drawable<?> out = deck.remove(i);
				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> drawSenshi() {
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Senshi s) {
				if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
					modHP(-10);
				}

				Drawable<?> out = deck.remove(i);
				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public Drawable<?> drawEvogear() {
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Evogear e) {
				if (origin.synergy() == Race.EX_MACHINA && !e.isSpell()) {
					modHP(50);
				}
				if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
					modHP(-10);
				}

				Drawable<?> out = deck.remove(i);
				cards.add(out);
				getGame().trigger(Trigger.ON_DRAW);
				return out;
			}
		}

		return null;
	}

	public LinkedList<Drawable<?>> getGraveyard() {
		graveyard.removeIf(d -> !d.getHand().equals(this));

		return graveyard;
	}

	public List<Drawable<?>> getDiscard() {
		return discard;
	}

	public void flushDiscard() {
		discard.removeIf(d -> !d.isSolid());
		graveyard.addAll(discard);
		discard.clear();
	}

	public Set<Timed<Lock>> getLocks() {
		return locks;
	}

	public int getLockTime(Lock lock) {
		return locks.stream()
				.filter(t -> t.obj().equals(lock))
				.map(Timed::time)
				.mapToInt(AtomicInteger::get)
				.findFirst().orElse(0);
	}

	public Set<EffectHolder<?>> getLeeches() {
		leeches.removeIf(e -> !Objects.equals(e.getLeech(), this));

		return leeches;
	}

	public BaseValues getBase() {
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
		this.hp = Math.max(0, hp);
	}

	public void modHP(int value) {
		modHP(value, false);
	}

	public void modHP(int value, boolean pure) {
		if (value == 0) return;

		int before = hp;

		if (!pure) {
			if (origin.major() == Race.HUMAN && value > 0) {
				value *= 1.25;
			} else if (origin.minor() == Race.HUMAN && value < 0) {
				value *= 1 - Math.min(game.getTurn() * 0.01, 0.75);
			}

			if (origin.synergy() == Race.POSSESSED && value > 0) {
				value *= 1 + game.getHands().get(side.getOther()).getGraveyard().size();
			} else if (origin.synergy() == Race.PRIMAL && value < 0) {
				int degen = Math.abs(value / 10);
				if (degen > 0) {
					regdeg.add(new Degen(degen, 0.1));
					value += degen;
				}
			}

			int half = value / 2;
			if (value < 0) {
				value = regdeg.reduce(Degen.class, value - half);
			} else {
				value = regdeg.reduce(Regen.class, value - half);
			}
			value += half;

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

		this.hp = Math.max(0, this.hp + value);

		if (!pure) {
			int delta = before - this.hp;
			if (delta > 0) {
				game.trigger(Trigger.ON_DAMAGE, side);

				if (origin.synergy() == Race.VIRUS) {
					modMP((int) (delta * 0.0025));
				} else if (origin.synergy() == Race.TORMENTED) {
					game.getHands().get(side.getOther()).modHP((int) -(delta * 0.01));
				}
			} else if (delta < 0) {
				game.trigger(Trigger.ON_HEAL, side);
			}
		}
	}

	public void consumeHP(int value) {
		this.hp = Math.max(0, this.hp - Math.max(0, value));
	}

	public double getHPPrcnt() {
		return hp / (double) base.hp();
	}

	public boolean isLowLife() {
		return origin.demon() || getHPPrcnt() <= 0.5;
	}

	public RegDeg getRegDeg() {
		return regdeg;
	}

	public void applyVoTs() {
		int val = regdeg.next();

		if (val < 0 && origin.major() == Race.HUMAN) {
			val /= 2;
		}

		modHP(val, true);
	}

	public JSONObject getData() {
		return data;
	}

	public int getMP() {
		return mp;
	}

	public void setMP(int mp) {
		this.mp = Math.max(0, mp);
	}

	public void modMP(int value) {
		this.mp = Math.max(0, this.mp + value);
	}

	public void consumeMP(int value) {
		if (origin.synergy() == Race.ESPER && Calc.chance(2)) return;

		this.mp = Math.max(0, this.mp - Math.max(0, value));
	}

	public void consumeSC(int value) {
		for (int i = 0; i < value && !discard.isEmpty(); i++) {
			game.getArena().getBanned().add(discard.remove(0));
		}
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
		return forfeit;
	}

	public void setForfeit(boolean forfeit) {
		this.forfeit = forfeit;
	}

	public int getMinorCooldown() {
		return Bit.get(cooldown, 0, 8);
	}

	public void setMinorCooldown(int time) {
		cooldown = Bit.set(cooldown, 0, time, 8);
	}

	public void reduceMinorCooldown(int time) {
		int curr = Bit.get(cooldown, 0, 8);
		cooldown = Bit.set(cooldown, 0, Math.max(0, curr - time), 8);
	}

	public int getMajorCooldown() {
		return Bit.get(cooldown, 1, 8);
	}

	public void setMajorCooldown(int time) {
		cooldown = Bit.set(cooldown, 1, time, 8);
	}

	public void reduceMajorCooldown(int time) {
		int curr = Bit.get(cooldown, 1, 8);
		cooldown = Bit.set(cooldown, 1, Math.max(0, curr - time), 8);
	}

	public int getKills() {
		return kills;
	}

	public void addKill() {
		this.kills++;
	}

	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage((225 + 20) * Math.max(5, cards.size()), 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 90));

		int offset = bi.getWidth() / 2 - ((225 + 20) * cards.size()) / 2;
		for (int i = 0; i < cards.size(); i++) {
			int x = offset + 10 + (225 + 10) * i;

			Drawable<?> d = cards.get(i);
			g2d.drawImage(d.render(locale, userDeck), x, bi.getHeight() - 350, null);
			if (d.isAvailable()) {
				Graph.drawOutlinedString(g2d, String.valueOf(i + 1),
						x + (225 / 2 - g2d.getFontMetrics().stringWidth(String.valueOf(i + 1)) / 2), 90,
						6, Color.BLACK
				);
			}
		}

		g2d.dispose();

		return bi;
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
