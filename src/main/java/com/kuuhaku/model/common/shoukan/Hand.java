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
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedLinkedList;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.CardType;
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
import com.kuuhaku.util.*;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Hand {
	private final long SERIAL = ThreadLocalRandom.current().nextLong();

	private final String uid;
	private final Shoukan game;
	private final Deck userDeck;

	private final Side side;
	private final Origin origin;

	private final List<Drawable<?>> cards = new BondedList<>((d, it) -> {
		if (getGame().getArena().getBanned().contains(this)) return false;

		d.setHand(this);
		getGame().trigger(Trigger.ON_HAND, d.asSource(Trigger.ON_HAND));

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			for (Evogear e : s.getEquipments()) {
				it.add(e);
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.setSlot(null);
		return true;
	});
	private final LinkedList<Drawable<?>> deck = new BondedLinkedList<>((d, it) -> {
		if (getGame().getArena().getBanned().contains(this)) return false;

		d.setHand(this);
		getGame().trigger(Trigger.ON_DECK, d.asSource(Trigger.ON_DECK));

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			for (Evogear e : s.getEquipments()) {
				it.add(e);
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();
		return true;
	});
	private final LinkedList<Drawable<?>> graveyard = new BondedLinkedList<>((d, it) -> {
		if (getGame().getArena().getBanned().contains(this)) return false;

		if (d instanceof Senshi s) {
			Evogear ward = null;
			for (Evogear e : s.getEquipments()) {
				if (e.hasCharm(Charm.WARDING)) {
					ward = e;
				}
			}

			if (s.getStats().popFlag(Flag.NO_DEATH)) {
				return false;
			} else if (ward != null) {
				int charges = ward.getStats().getData().getInt("uses", 0) + 1;
				if (charges >= Charm.WARDING.getValue(ward.getTier())) {
					it.add(ward);
				} else {
					ward.getStats().getData().put("uses", charges);
				}

				return false;
			}
		}

		d.setHand(this);
		getGame().trigger(Trigger.ON_GRAVEYARD, d.asSource(Trigger.ON_GRAVEYARD));

		if (getGame().getCurrentSide() != getSide()) {
			Hand op = getOther();

			if (op.kills % 7 == 0 && op.getOrigin().synergy() == Race.SHINIGAMI) {
				getGame().getArena().getBanned().add(d);
				return false;
			}

			op.kills++;
		}

		if (d instanceof Senshi s && !s.getEquipments().isEmpty()) {
			for (Evogear e : s.getEquipments()) {
				it.add(e);
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();

		if (d.getHand().getOrigin().synergy() == Race.REBORN && Calc.chance(5)) {
			cards.add(d.copy());
			return false;
		}

		return d.keepOnDestroy();
	});
	private final List<Drawable<?>> discard = new BondedList<>((d, it) -> {
		if (getGame().getArena().getBanned().contains(this)) return false;

		d.setHand(this);
		getGame().trigger(Trigger.ON_DISCARD, d.asSource(Trigger.ON_DISCARD));
		d.setAvailable(false);

		return d.keepOnDestroy();
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
	private transient String defeat;
	private transient int kills = 0;
	private transient byte cooldown = 0;
	/*
	0xF
	  └ (0 - 127) origin effect
	 */

	private transient Pair<List<Drawable<?>>, CompletableFuture<Drawable<?>>> selection = null;

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
		this.origin = Utils.getOr(game.getParams().origin(), userDeck.getOrigins());
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

		for (String card : game.getParams().cards()) {
			card = card.toUpperCase();
			CardType type = Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", card)).stream()
					.findFirst()
					.orElse(CardType.NONE);

			Drawable<?> d = switch (type) {
				case NONE -> null;
				case KAWAIPON -> DAO.find(Senshi.class, card);
				case EVOGEAR -> DAO.find(Evogear.class, card);
				case FIELD -> DAO.find(Field.class, card);
			};
			if (d == null) continue;

			cards.add(d);
		}
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

	public List<Drawable<?>> getCards() {
		cards.removeIf(d -> !equals(d.getHand()));

		return cards;
	}

	public int getHandCount() {
		return (int) cards.stream().filter(Drawable::isSolid).count();
	}

	public int getRemainingDraws() {
		return Math.max(0, base.handCapacity().apply(game.getTurn()) - getHandCount());
	}

	public LinkedList<Drawable<?>> getRealDeck() {
		deck.removeIf(d -> !equals(d.getHand()));

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
					if (getOther().getOrigin().synergy() == Race.IMP) {
						modHP(-10);
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

		for (int i = 0; i < value; i++) {
			Drawable<?> d = deck.pollFirst();

			if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
				modHP(50);
			}
			if (getOther().getOrigin().synergy() == Race.IMP) {
				modHP(-10);
			}

			if (d != null) {
				d.setSolid(true);
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
		if (getOther().getOrigin().synergy() == Race.IMP) {
			modHP(-10);
		}

		if (d != null) {
			d.setSolid(true);
			cards.add(d);
			getGame().trigger(Trigger.ON_DRAW);
		}

		return d;
	}

	public List<Drawable<?>> draw(int value) {
		List<Drawable<?>> out = new ArrayList<>();
		for (int i = 0; i < value; i++) {
			Drawable<?> d = draw();
			if (d == null) return out;

			out.add(d);
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
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-10);
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
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Senshi s && s.getRace().isRace(race)) {
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-10);
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
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			Drawable<?> d = deck.get(i);
			if (cond.test(d)) {
				if (origin.synergy() == Race.EX_MACHINA && d instanceof Evogear e && !e.isSpell()) {
					modHP(50);
				}
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-10);
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
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Senshi s) {
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-10);
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
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Evogear e) {
				if (origin.synergy() == Race.EX_MACHINA && !e.isSpell()) {
					modHP(50);
				}
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-10);
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
		LinkedList<Drawable<?>> deck = getDeck();

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
		LinkedList<Drawable<?>> deck = getDeck();

		for (int i = 0; i < deck.size(); i++) {
			if (deck.get(i) instanceof Evogear e && e.isSpell()) {
				if (getOther().getOrigin().synergy() == Race.IMP) {
					modHP(-10);
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

	public LinkedList<Drawable<?>> getGraveyard() {
		graveyard.removeIf(d -> !equals(d.getHand()));

		return graveyard;
	}

	public List<Drawable<?>> getDiscard() {
		discard.removeIf(d -> !cards.contains(d));

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

		int before = hp;

		if (!pure) {
			if (origin.major() == Race.HUMAN && value > 0) {
				value *= 1.25;
			} else if (origin.hasMinor(Race.HUMAN) && value < 0) {
				value *= 1 - Math.min(game.getTurn() * 0.01, 0.75);
			}

			if (origin.synergy() == Race.POSSESSED && value > 0) {
				value *= 1 + getOther().getGraveyard().size();
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

		this.hp = Utils.clamp(this.hp + value, 0, base.hp() * 2);

		if (!pure) {
			int delta = before - this.hp;
			if (delta > 0) {
				game.trigger(Trigger.ON_DAMAGE, side);

				if (origin.synergy() == Race.VIRUS) {
					modMP((int) (delta * 0.0025));
				} else if (origin.synergy() == Race.TORMENTED) {
					getOther().modHP((int) -(delta * 0.01));
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
		return origin.demon() || getHPPrcnt() < 0.5;
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
		if (origin.major() == Race.DEMON) {
			return Math.max(0, hp / (base.hp() / 10) - 1);
		}

		return mp;
	}

	public void setMP(int mp) {
		if (origin.major() == Race.DEMON) {
			setHP((mp + 1) * (base.hp() / 10));
			return;
		}

		this.mp = Utils.clamp(mp, 0, 99);
	}

	public void modMP(int value) {
		if (origin.major() == Race.DEMON) {
			modHP(value * (base.hp() / 10));
			return;
		}

		this.mp = Utils.clamp(this.mp + value, 0, 99);
	}

	public void consumeMP(int value) {
		if (origin.synergy() == Race.ESPER && Calc.chance(2)) return;
		else if (origin.major() == Race.DEMON) {
			consumeHP(value * (base.hp() / 10));
			return;
		}

		this.mp = Utils.clamp(this.mp - value, 0, 99);
	}

	public List<Drawable<?>> consumeSC(int value) {
		List<Drawable<?>> consumed = new ArrayList<>();

		for (int i = 0; i < value && !discard.isEmpty(); i++) {
			Drawable<?> card = discard.remove(0);

			consumed.add(card);
			game.getArena().getBanned().add(card);
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
		return forfeit;
	}

	public void setForfeit(boolean forfeit) {
		this.forfeit = forfeit;
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
		return Bit.get(cooldown, 0, 8);
	}

	public void setOriginCooldown(int time) {
		cooldown = (byte) Bit.set(cooldown, 0, time, 8);
	}

	public void reduceOriginCooldown(int time) {
		byte curr = (byte) Bit.get(cooldown, 1, 8);
		cooldown = (byte) Bit.set(cooldown, 1, Math.max(0, curr - time), 8);
	}

	public int getKills() {
		return kills;
	}

	public void addKill() {
		kills++;
	}

	public BufferedImage render(I18N locale) {
		return render(locale, true);
	}

	public BufferedImage render(I18N locale, boolean ally) {
		BufferedImage bi = new BufferedImage((225 + 20) * Math.max(5, cards.size()), 480, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 90));

		int offset = bi.getWidth() / 2 - ((225 + 20) * cards.size()) / 2;
		for (int i = 0; i < cards.size(); i++) {
			int x = offset + 10 + (225 + 10) * i;

			Drawable<?> d = cards.get(i);
			g2d.drawImage(d.render(locale, userDeck), x, bi.getHeight() - 380, null);
			if (d.isAvailable() && ally) {
				Graph.drawOutlinedString(g2d, String.valueOf(i + 1),
						x + (225 / 2 - g2d.getFontMetrics().stringWidth(String.valueOf(i + 1)) / 2), 90,
						6, Color.BLACK
				);
			}

			if (!ally) {
				Graph.applyTransformed(g2d, x, bi.getHeight() - 380, g -> {
					g.setClip(userDeck.getStyling().getFrame().getBoundary());
					g.drawImage(IO.getResourceAsImage("shoukan/states/sight.png"), 0, 0, null);
				});
			}

			if ((d instanceof Senshi s && s.getStats().hasFlag(Flag.EMPOWERED)) || (d instanceof Evogear e && e.getStats().hasFlag(Flag.EMPOWERED))) {
				boolean legacy = userDeck.getStyling().getFrame().isLegacy();
				BufferedImage emp = IO.getResourceAsImage("kawaipon/frames/" + (legacy ? "old" : "new") + "/empowered.png");

				g2d.drawImage(emp, x, bi.getHeight() - 380, null);
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
				.flatMap(chn -> chn.sendFile(IO.getBytes(hand.render(game.getLocale(), equals(hand)), "png"), "hand.png"))
				.queue(m -> {
					if (equals(hand)) {
						if (lastMessage != null) {
							m.getChannel().retrieveMessageById(lastMessage)
									.flatMap(Objects::nonNull, Message::delete)
									.queue();
						}

						lastMessage = m.getId();
					}
				});
	}

	public CompletableFuture<Drawable<?>> requestChoice(List<Drawable<?>> cards) {
		if (cards.isEmpty()) throw new ActivationException(game.getString("err/empty_selection"));
		selection = new Pair<>(cards, new CompletableFuture<>());

		BufferedImage bi = new BufferedImage((225 + 20) * Math.max(5, cards.size()), 550, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 90));
		g2d.translate(0, 100);

		String str = game.getLocale().get("str/select_a_card");
		Graph.drawOutlinedString(g2d, str,
				bi.getWidth() / 2 - g2d.getFontMetrics().stringWidth(str) / 2, -10,
				6, Color.BLACK
		);

		int offset = bi.getWidth() / 2 - ((225 + 20) * cards.size()) / 2;
		for (int i = 0; i < cards.size(); i++) {
			int x = offset + 10 + (225 + 10) * i;

			Drawable<?> d = cards.get(i);
			g2d.drawImage(d.render(game.getLocale(), userDeck), x, bi.getHeight() - 450, null);
			if (d.isAvailable()) {
				Graph.drawOutlinedString(g2d, String.valueOf(i + 1),
						x + (225 / 2 - g2d.getFontMetrics().stringWidth(String.valueOf(i + 1)) / 2), 90,
						6, Color.BLACK
				);
			}
		}

		g2d.dispose();

		Message msg = Pages.subGet(getUser().openPrivateChannel().flatMap(chn -> chn.sendFile(IO.getBytes(bi, "png"), "choices.png")));

		selection.getSecond().thenAccept(d -> {
			msg.delete().queue(null, Utils::doNothing);
			selection = null;
		});

		return selection.getSecond();
	}

	public void requestChoice(Predicate<Drawable<?>> cond, Consumer<Drawable<?>> act) {
		requestChoice(cards.stream().filter(cond).toList()).thenAccept(act);
	}

	public void requestChoice(List<Drawable<?>> cards, Consumer<Drawable<?>> act) {
		requestChoice(cards).thenAccept(act);
	}

	public Pair<List<Drawable<?>>, CompletableFuture<Drawable<?>>> getSelection() {
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
