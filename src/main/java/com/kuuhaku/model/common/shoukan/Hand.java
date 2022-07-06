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
import com.kuuhaku.interfaces.AccFunction;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.BondedLinkedList;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.model.records.shoukan.RegDeg;
import com.kuuhaku.model.records.shoukan.Timed;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import kotlin.Triple;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Hand {
	private final long timestamp = System.currentTimeMillis();

	private final String uid;
	private final Shoukan game;
	private final Deck userDeck;

	private final Side side;
	private final Origin origin;

	private final List<Drawable<?>> cards = new BondedList<>(d -> d.setHand(this));
	private final LinkedList<Drawable<?>> deck = new BondedLinkedList<>(d -> d.setHand(this));
	private final LinkedList<Drawable<?>> graveyard = new BondedLinkedList<>(d -> {
		d.reset();

		if (d.getHand().getOrigin().synergy() == Race.REBORN && Calc.chance(1)) {
			cards.add(d.copy());
			d.setSolid(false);
		}

		getGraveyard().removeIf(dr -> !dr.isSolid());
	});
	private final List<Drawable<?>> discard = new BondedList<>(d -> d.setAvailable(false));
	private final Set<Timed<Lock>> locks = new HashSet<>();

	private final BaseValues base;
	private final List<RegDeg> regdeg = new ArrayList<>();

	private String name;

	private int hp;
	private int mp;

	private transient Account account;
	private transient String lastMessage;
	private transient boolean forfeit;
	private transient int cooldown;
	private transient int kills = 0;

	public Hand(String uid, Shoukan game, Side side) {
		this.uid = uid;
		this.game = game;
		this.userDeck = DAO.find(Account.class, uid).getCurrentDeck();
		this.side = side;
		this.origin = userDeck.getOrigins();

		BaseValues base;
		try {
			base = new BaseValues(() -> {
				int bHP = 5000;
				AccFunction<Integer, Integer> mpGain = t -> 5;
				int handCap = 5;

				mpGain = switch (origin.major()) {
					case DEMON -> {
						bHP -= 1500;
						yield mpGain.accumulate((t, mp) -> mp + (int) (5 - 5 * getHPPrcnt()));
					}
					case DIVINITY -> mpGain.accumulate((t, mp) -> mp + (int) (mp * userDeck.getMetaDivergence() / 2));
					default -> mpGain;
				};

				if (origin.synergy() == Race.FEY && Calc.chance(1)) {
					mpGain = mpGain.accumulate((t, mp) -> mp * 2);
				} else if (origin.synergy() == Race.GHOST) {
					mpGain = mpGain.accumulate((t, mp) -> mp + (t % 10 == 0 ? 1 : 0));
				}

				return new Triple<>(bHP, mpGain, handCap);
			});
		} catch (Exception e) {
			base = new BaseValues();
		}
		this.base = base;
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
		return Main.getApp().getShiro().getUserById(uid);
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
		return cards;
	}

	public int getHandCount() {
		return (int) cards.stream().filter(Drawable::isSolid).count();
	}

	public int getRemainingDraws() {
		return Math.max(0, base.handCapacity() - getHandCount());
	}

	public LinkedList<Drawable<?>> getDeck() {
		return deck;
	}

	public boolean manualDraw(int value) {
		if (deck.isEmpty()) return false;

		if (cards.stream().noneMatch(d -> d instanceof Senshi)) {
			for (int i = 0; i < deck.size() && value > 0; i++) {
				if (deck.get(i) instanceof Senshi) {
					if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
						modHP(-10);
					}

					cards.add(deck.remove(i));
					value--;
					break;
				}
			}
		}

		for (int i = 0; i < value; i++) {
			draw();
		}

		return true;
	}

	public void draw() {
		Drawable<?> d = deck.removeFirst();

		if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
			modHP(50);
		}
		if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
			modHP(-10);
		}

		cards.add(d);
	}

	public void draw(int value) {
		for (int i = 0; i < value; i++) {
			draw();
		}
	}

	public void drawSenshi(int value) {
		for (int i = 0; i < deck.size() && value > 0; i++) {
			if (deck.get(i) instanceof Senshi) {
				if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
					modHP(-10);
				}

				cards.add(deck.remove(i));
				value--;
			}
		}
	}

	public void drawEvogear(int value) {
		for (int i = 0; i < deck.size() && value > 0; i++) {
			if (deck.get(i) instanceof Evogear e) {
				if (origin.synergy() == Race.EX_MACHINA && !e.isSpell()) {
					modHP(50);
				}
				if (game.getHands().get(side.getOther()).getOrigin().synergy() == Race.IMP) {
					modHP(-10);
				}

				cards.add(deck.remove(i));
				value--;
			}
		}
	}

	public LinkedList<Drawable<?>> getGraveyard() {
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
		int before = hp;

		if (origin.major() == Race.HUMAN && value > 0) {
			value *= 1.25;
		} else if (origin.major() == Race.HUMAN && value < 0) {
			value *= 1 - Math.min(game.getTurn() * 0.01, 0.75);
		}

		if (origin.synergy() == Race.POSSESSED) {
			value *= 1 + game.getHands().get(side.getOther()).getGraveyard().size();
		}

		RegDeg rd = null;
		if (value < 0) {
			rd = regdeg.stream()
					.filter(r -> r.remaining() > 0)
					.findFirst().orElse(null);
		} else if (value > 0) {
			rd = regdeg.stream()
					.filter(d -> d.remaining() < 0)
					.findFirst().orElse(null);
		}

		if (rd != null) {
			value = rd.reduce(value);
		}

		double prcnt = getHPPrcnt();
		if (this.hp + value < 0 && prcnt > 1 / 3d) {
			if (prcnt > 2 / 3d || Calc.chance(prcnt * 100)) {
				this.hp = 1;
				return;
			}
		}

		this.hp = Math.max(0, this.hp + value);

		int delta = before - this.hp;
		if (origin.synergy() == Race.VIRUS) {
			modMP((int) (delta * 0.005));
		} else if (origin.synergy() == Race.TORMENTED) {
			game.getHands().get(side.getOther()).modHP((int) -(delta * 0.01));
		}
	}

	public void consumeHP(int value) {
		modHP(-value);
	}

	public double getHPPrcnt() {
		if (origin.demon()) {
			return Math.min(hp / (double) base.hp(), 0.5);
		}

		return hp / (double) base.hp();
	}

	public List<RegDeg> getRegdeg() {
		return regdeg;
	}

	public int getRegen() {
		return regdeg.stream()
				.mapToInt(RegDeg::remaining)
				.filter(i -> i > 0)
				.sum();
	}

	public void addRegen(int regen, double dpt) {
		regen = Math.min(regen, base.hp() - getRegen());

		this.regdeg.add(new RegDeg(Math.max(0, regen), dpt));
	}

	public void applyRegen() {
		Iterator<RegDeg> it = regdeg.iterator();
		while (it.hasNext()) {
			RegDeg rd = it.next();
			if (rd.remaining() < 0) continue;

			modHP(rd.slice());
			if (rd.remaining() <= 0) {
				it.remove();
			}
		}
	}

	public int getDegen() {
		return regdeg.stream()
				.mapToInt(RegDeg::remaining)
				.filter(i -> i < 0)
				.map(Math::abs)
				.sum();
	}

	public void addDegen(int degen, double dpt) {
		degen = Math.min(degen, base.hp() - getRegen());

		this.regdeg.add(new RegDeg(Math.min(degen, 0), dpt));
	}

	public void applyDegen() {
		Iterator<RegDeg> it = regdeg.iterator();
		while (it.hasNext()) {
			RegDeg rd = it.next();
			if (rd.remaining() > 0) continue;

			if (origin.major() == Race.HUMAN) {
				modHP(rd.slice() / 2);
			} else {
				modHP(rd.slice());
			}
			if (rd.remaining() >= 0) {
				it.remove();
			}
		}
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
		if (origin.synergy() == Race.FETCH && Calc.chance(1)) return;

		modMP(-value);
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

	public int getCooldown() {
		return cooldown;
	}

	public void setCooldown(int cooldown) {
		this.cooldown = cooldown;
	}

	public void reduceCooldown() {
		this.cooldown = Math.max(0, cooldown - 1);
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
		g2d.setFont(new Font("Arial", Font.BOLD, 90));

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
		return timestamp == hand.timestamp && Objects.equals(uid, hand.uid) && side == hand.side && Objects.equals(origin, hand.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, uid, side, origin);
	}
}
