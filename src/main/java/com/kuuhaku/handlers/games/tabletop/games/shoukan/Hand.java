/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.annotations.ExecTime;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.BondedList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.*;

public class Hand {
	private final Shoukan game;
	private final Account acc;
	private final Side side;
	private final Map<Race, Long> raceCount;
	private final BondedList<Drawable> deque;
	private final BondedList<Drawable> cards;
	private final BondedList<Drawable> destinyDeck;
	private final BondedList<Drawable> discardBatch;
	private final Hero hero;
	private final Triple<Race, Boolean, Race> combo;
	private final double divergence;
	private final double avgCost;
	private int baseHp;
	private int baseManaPerTurn;
	private float mitigation = 0;
	private int maxCards = 0;
	private int manaPerTurn = 0;
	private int mana = 0;
	private int hp = 0;
	private int prevHp = 0;
	private int suppressTime = 0;
	private int lockTime = 0;
	private int nullTime = 0;
	private int blindTime = 0;
	private int hideMana = 0;
	private float healingFac = 1;
	private int bleeding = 0;
	private int regeneration = 0;
	private Message old = null;

	@ExecTime
	public Hand(Shoukan game, Side side) {
		this(game, null, null, side);
	}

	@ExecTime
	public Hand(Shoukan game, User user, Deck dk, Side side) {
		this.game = game;
		this.side = side;
		this.discardBatch = new BondedList<>(d -> {
			Side[] sides = {side, side.getOther()};
			for (int i = 0; i < 5; i++) {
				for (Side s : sides) {
					SlotColumn slot = game.getSlots().get(s).get(i);
					if (slot.getTop() == null) continue;

					Champion c = slot.getTop();
					game.applyEffect(side == game.getCurrentSide() ? ON_DISCARD : ON_OP_DISCARD, c, s, -1);
				}
			}
		});

		if (user == null) {
			this.acc = null;
			this.deque = null;
			this.cards = null;
			this.destinyDeck = null;
			this.combo = null;
			this.divergence = 0;
			this.avgCost = 0;
			this.raceCount = null;
			this.hero = null;
			return;
		}

		this.acc = AccountDAO.getAccount(user.getId());

		Hero h = KawaiponDAO.getHero(user.getId());
		if (h != null && h.getQuest() == null) {
			this.hero = h;
		} else {
			this.hero = null;
		}

		Consumer<Drawable> bonding = d -> {
			d.bind(this);

			if (getCombo().getRight() == Race.DIVINITY) {
				if (d instanceof Champion c && !c.hasEffect() && !c.isFusion()) {
					String[] de = CardDAO.getRandomEffect(c.getMana());
					c.setAltDescription(de[0]);
					c.setAltEffect(de[1]);
				}
			}
		};
		this.deque = new BondedList<>(bonding);
		this.cards = new BondedList<>(bonding);
		this.destinyDeck = new BondedList<>(bonding);
		this.combo = dk.getCombo();
		this.divergence = dk.getAverageDivergence();
		this.avgCost = dk.getAverageCost();
		this.mitigation = Math.min(
				combo.getRight() == Race.HUMAN
						? dk.getChampions().stream()
						.filter(c -> c.getMana() <= 2)
						.count() * 0.01f
						: 0, 0.5f
		);

		game.getDivergence().put(user.getId(), dk.getAverageDivergence());

		raceCount = dk.getChampions().stream()
				.collect(Collectors.groupingBy(Champion::getRace, Collectors.counting()));

		setData(
				dk.getChampions(),
				dk.getEquipments(),
				dk.getFields(),
				dk.getDestinyDraw()
		);
	}

	private void setData(List<Champion> champs, List<Equipment> equips, List<Field> fields, List<Integer> destinyDraw) {
		deque.addAll(
				Stream.of(champs, equips, fields)
						.flatMap(List::stream)
						.map(Drawable::copy)
						.toList()
		);
		if (hero != null) deque.add(hero.toChampion());

		int baseHp = game.getRules().baseHp();
		int baseManaPerTurn = game.getRules().baseManaPerTurn();
		int maxCards = game.getRules().maxCards();

		mana = game.getRules().mana();

		if (game.getRules().noEquip())
			getRealDeque().removeIf(d -> d instanceof Equipment e && !e.isSpell());
		if (game.getRules().noSpell())
			getRealDeque().removeIf(d -> d instanceof Equipment e && e.isSpell());
		if (game.getRules().noField())
			getRealDeque().removeIf(d -> d instanceof Field);

		switch (game.getRules().arcade()) {
			case ROULETTE -> {
				for (Drawable d : deque) {
					if (d instanceof Champion c) {
						c.addCurse("""
								import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger
																
								if (ep.getTrigger() == EffectTrigger.ON_ATTACK) {
									int rng = Math.round(Math.random() * 100) as int
									if (rng < 25) {
										Hand h = ep.getHands().get(ep.getSide())
										h.setHp(h.getHp() / 2)
									} else if (rng < 50) {
										Hand h = ep.getHands().get(ep.getSide().getOther())
										h.setHp(h.getHp() / 2)
									}
								}
								""");
					}
				}
			}
			case BLACKROCK -> {
				deque.removeIf(d -> d instanceof Champion || d instanceof Field);
				for (String name : new String[]{"MATO_KUROI", "SAYA_IRINO", "YOMI_TAKANASHI", "YUU_KOUTARI", "TAKU_KATSUCHI", "KAGARI_IZURIHA"}) {
					Champion c = CardDAO.getChampion(name);
					deque.addAll(Collections.nCopies(6, c));
				}
			}
			case INSTAKILL -> baseHp = 1;
			case CARDMASTER -> {
				deque.clear();
				deque.addAll(CardDAO.getAllChampions(false));
				deque.addAll(CardDAO.getAllAvailableEquipments());
				deque.addAll(CardDAO.getAllAvailableFields());
			}
		}

		if (destinyDraw != null) {
			for (int i : destinyDraw) {
				if (i > champs.size()) {
					destinyDeck.clear();
					break;
				} else {
					destinyDeck.add(champs.get(i));
				}
			}
		}
		for (Drawable drawable : destinyDeck) {
			deque.remove(drawable);
		}

		int hpMod = combo.getLeft() == Race.DEMON ? -1500 : 0;
		int manaMod = combo.getLeft() == Race.ELF ? 1 : 0;

		this.baseHp = hp = Math.max(baseHp + hpMod, 1);
		this.baseManaPerTurn = manaPerTurn = Math.max(baseManaPerTurn + manaMod, 0);
		this.maxCards = Math.max(maxCards + (combo.getRight() == Race.CREATURE ? 1 : 0), 1);
		redrawHand();
	}

	public boolean manualDraw() {
		try {
			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() >= 4 && getRealDeque().stream().anyMatch(d -> d instanceof Champion))
				manualDrawChampion();
			else cards.add(getRealDeque().removeFirst().copy());
			triggerEffect(ON_DRAW);
			triggerEffect(ON_MANUAL_DRAW);

			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public void destinyDraw() {
		if (destinyDeck.size() > 0) {
			Drawable dr = Helper.getRandomEntry(destinyDeck);
			destinyDeck.remove(dr);
			cards.add(dr.copy());
			deque.addAll(destinyDeck);
			destinyDeck.clear();
			triggerEffect(ON_DRAW);
		}
	}

	public Drawable draw() {
		if (lockTime > 0) return null;
		try {
			Drawable dr;
			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() == 4 && getRealDeque().stream().anyMatch(d -> d instanceof Champion))
				dr = drawChampion();
			else {
				dr = getRealDeque().removeFirst();
				cards.add(dr.copy());
			}
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(Card card) {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(Drawable drawable) {
		if (lockTime > 0) return null;
		Card card = drawable.getCard();
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(String name) {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c.getCard().getId().equals(name)).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawChampion() {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawChampion(int mana) {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(d -> d instanceof Champion c && c.getMana() == mana).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public void manualDrawChampion() {
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);
			triggerEffect(ON_MANUAL_DRAW);
		} catch (NoSuchElementException ignore) {
		}
	}

	public Drawable drawEquipment() {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Equipment e && !e.isSpell()).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawSpell() {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Equipment e && e.isSpell()).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawHighest(boolean attack) {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream()
					.filter(c -> c instanceof Equipment e && !e.isSpell())
					.max(Comparator.comparingInt(c -> attack ? ((Equipment) c).getAtk() : ((Equipment) c).getDef()))
					.orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawField() {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Field).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawRace(Race race) {
		if (lockTime > 0) return null;
		try {
			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Champion && ((Champion) c).getRace() == race).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public void redrawHand() {
		List<Drawable> notUsed = cards.stream().filter(Drawable::isAvailable).toList();
		deque.addAll(notUsed);
		cards.removeIf(Drawable::isAvailable);

		Collections.shuffle(deque);
		int toDraw = Math.max(0, maxCards - getCardCount());
		for (int i = 0; i < toDraw; i++) manualDraw();
	}

	public Shoukan getGame() {
		return game;
	}

	public Account getAcc() {
		return acc;
	}

	public User getUser() {
		User u = Main.getInfo().getUserByID(acc.getUid());
		if (!u.getId().equals(acc.getUid())) return getUser();

		return u;
	}

	public Map<Race, Long> getRaceCount() {
		return raceCount;
	}

	public Triple<Race, Boolean, Race> getCombo() {
		return combo;
	}

	public double getDivergence() {
		return divergence;
	}

	public double getAvgCost() {
		return avgCost;
	}

	public BondedList<Drawable> getDeque() {
		if (lockTime > 0) return new BondedList<>(deque.getBonding());
		else return getRealDeque();
	}

	public BondedList<Drawable> getRealDeque() {
		if (deque.isEmpty()) {
			deque.addAll(destinyDeck);
			destinyDeck.clear();
		}
		return deque;
	}

	public BondedList<Drawable> getCards() {
		return cards;
	}

	public boolean hasCard(String id) {
		return getAvailableCards().stream()
				.map(d -> d.getCard().getId())
				.anyMatch(id::equalsIgnoreCase);
	}

	public List<Champion> getSortedChampions() {
		return getAvailableCards().stream()
				.filter(d -> d instanceof Champion)
				.map(d -> (Champion) d)
				.sorted(Comparator.<Champion>comparingInt(Champion::getMana)
						.thenComparing(Champion::getAtk)
						.thenComparing(Champion::getDef)
				).collect(Collectors.toList());
	}

	public List<Equipment> getSortedEquipments() {
		return getAvailableCards().stream()
				.filter(d -> d instanceof Equipment)
				.map(d -> (Equipment) d)
				.sorted(Comparator.comparingInt(Equipment::getMana)
						.thenComparing(Equipment::getAtk)
						.thenComparing(Equipment::getDef)
				).collect(Collectors.toList());
	}

	public List<Drawable> getAvailableCards() {
		return getCards().stream()
				.filter(Drawable::isAvailable)
				.collect(Collectors.toList());
	}

	public void removeCard(String name) {
		getAvailableCards().removeIf(d -> d.getCard().getId().equalsIgnoreCase(name));
	}

	public BondedList<Drawable> getDestinyDeck() {
		return destinyDeck;
	}

	public BondedList<Drawable> getDiscardBatch() {
		return discardBatch;
	}

	public Hero getHero() {
		return hero;
	}

	public Side getSide() {
		return side;
	}

	public BufferedImage render() {
		BufferedImage bi = new BufferedImage(Math.max(5, cards.size()) * 300, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 0, null);
		}

		g2d.dispose();

		BufferedImage container = new BufferedImage(Math.max(2250, bi.getWidth()), 350, BufferedImage.TYPE_INT_ARGB);
		g2d = container.createGraphics();
		g2d.drawImage(bi, container.getWidth() / 2 - bi.getWidth() / 2, 0, null);
		g2d.dispose();

		return container;
	}

	public void showHand() {
		BufferedImage bi = new BufferedImage(Math.max(5, cards.size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 90));

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(isBlinded()), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			if (cards.get(i).isAvailable())
				Profile.printCenteredString(String.valueOf(i + 1), 225, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 90, g2d);
		}

		g2d.dispose();

		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage("Suas cartas:").addFile(Helper.writeAndGet(bi, "hand", "png")))
				.queue(m -> {
					if (old != null) old.delete().queue(null, Helper::doNothing);
					old = m;
				}, Helper::doNothing);
	}

	public void showEnemyHand() {
		Hand enemy = game.getHands().get(side.getOther());
		BufferedImage bi = new BufferedImage(Math.max(5, enemy.getCards().size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = enemy.getCards();

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			try {
				BufferedImage so = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/shinigami_overlay.png")));
				g2d.drawImage(so, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			} catch (IOException ignore) {
			}
		}

		g2d.dispose();

		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage("Visualizando as cartas na mão do oponente:")
						.addFile(Helper.writeAndGet(bi, "hand", "png"))
				)
				.queue(null, Helper::doNothing);
	}

	public void showEnemyHand(float hidden) {
		hidden = Helper.clamp(hidden, 0, 1);

		Hand enemy = game.getHands().get(side.getOther());
		BufferedImage bi = new BufferedImage(Math.max(5, enemy.getCards().size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = enemy.getCards();
		List<Integer> hidIndx = Helper.getRandomN(IntStream.range(0, cards.size()).boxed().toList(), (int) Math.ceil(cards.size() * hidden), 1);

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(hidIndx.contains(i)), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			try {
				BufferedImage so = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/shinigami_overlay.png")));
				g2d.drawImage(so, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			} catch (IOException ignore) {
			}
		}

		g2d.dispose();

		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage("Visualizando as cartas na mão do oponente:")
						.addFile(Helper.writeAndGet(bi, "hand", "png"))
				)
				.queue(null, Helper::doNothing);
	}

	public void showEnemyDeck(int amount) {
		Hand op = game.getHands().get(side.getOther());
		BufferedImage bi = new BufferedImage(Math.max(5, amount) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = op.getRealDeque().subList(0, amount);

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			try {
				BufferedImage so = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/shinigami_overlay.png")));
				g2d.drawImage(so, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			} catch (IOException ignore) {
			}
		}

		g2d.dispose();

		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage("Visualizando as próximas " + amount + " cartas do oponente:")
						.addFile(Helper.writeAndGet(bi, "hand", "png"))
				)
				.queue(null, Helper::doNothing);
	}

	public void showCards(Drawable... cards) {
		BufferedImage bi = new BufferedImage(Math.max(5, cards.length) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 90));

		for (int i = 0; i < cards.length; i++) {
			g2d.drawImage(cards[i].drawCard(false), bi.getWidth() / (cards.length + 1) * (i + 1) - (225 / 2), 100, null);
			try {
				BufferedImage so = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/shinigami_overlay.png")));
				g2d.drawImage(so, bi.getWidth() / (cards.length + 1) * (i + 1) - (225 / 2), 100, null);
			} catch (IOException ignore) {
			}
		}

		g2d.dispose();

		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage("Visualizando as cartas:")
						.addFile(Helper.writeAndGet(bi, "hand", "png"))
				)
				.queue(null, Helper::doNothing);
	}

	public void sendDM(String message) {
		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage(message))
				.queue(null, Helper::doNothing);
	}

	public int sumAttack() {
		return cards.stream().filter(d -> d instanceof Champion).mapToInt(d -> ((Champion) d).getAtk()).sum();
	}

	public float getMitigation() {
		return mitigation;
	}

	public void setMitigation(float mitigation) {
		this.mitigation = Math.min(mitigation, 0.5f);
	}

	public int getMaxCards() {
		return maxCards;
	}

	public int getCardCount() {
		return (int) cards.stream()
				.filter(d -> !(d instanceof Equipment e) || !e.isEffectOnly())
				.count();
	}

	public void setMaxCards(int maxCards) {
		this.maxCards = maxCards;
	}

	public int getMana() {
		return isSuppressed() ? 0 : Math.min(mana, 99);
	}

	public int getBaseManaPerTurn() {
		return baseManaPerTurn;
	}

	public void setBaseManaPerTurn(int baseManaPerTurn) {
		this.baseManaPerTurn = baseManaPerTurn;
	}

	public int getManaPerTurn() {
		return isSuppressed() ? 0 : manaPerTurn;
	}

	public void setManaPerTurn(int manaPerTurn) {
		this.manaPerTurn = manaPerTurn;
	}

	public void setMana(int value) {
		mana = Math.min(value, 99);
	}

	public void addMana(int value) {
		mana = Math.min(mana + (isSuppressed() ? 0 : value), 99);
	}

	public void removeMana(int value) {
		mana = Math.max(0, mana - value);
	}

	public int getBaseHp() {
		return Math.min(baseHp, 9999);
	}

	public void setBaseHp(int baseHp) {
		this.baseHp = Math.min(baseHp, 9999);
	}

	public int getHp() {
		return hp;
	}

	public void setHp(int value) {
		prevHp = hp;
		hp = Helper.clamp(value, 0, 9999);
	}

	public void addHp(int value) {
		addHp(value, true);
	}

	public void addHp(int value, boolean trigger) {
		if (value <= 0) return;

		setHp((int) (hp + value * getHealingFac()));
		decreaseBleeding((int) (value * 0.25));

		if (trigger) {
			triggerEffect(ON_HEAL);
		}
	}

	public void removeHp(int value) {
		removeHp(value, true);
	}

	public void removeHp(int value, boolean trigger) {
		if (value <= 0) return;

		if (combo.getMiddle()) value *= 1.25f;

		if (hp > 1) {
			if (hp > baseHp * (2 / 3f)) {
				crippleHp(value);
				return;
			} else if (hp > baseHp * (1 / 3f) && Helper.chance(hp * 100d / baseHp)) {
				crippleHp(value);
				return;
			}
		}

		setHp(hp - value);
		if (trigger) {
			triggerEffect(ON_DAMAGE);
		}
	}

	public void crippleHp(int value) {
		crippleHp(value, true);
	}

	public void crippleHp(int value, boolean trigger) {
		if (value <= 0) return;

		setHp(Math.max(1, hp - value));
		if (trigger) {
			triggerEffect(ON_DAMAGE);
		}
	}

	public int getPrevHp() {
		return prevHp;
	}

	public void setPrevHp(int prevHp) {
		this.prevHp = prevHp;
	}

	public int getHealDelta() {
		return Math.max(hp - prevHp, 0);
	}

	public int getDamageDelta() {
		return Math.max(prevHp - hp, 0);
	}

	public int getSuppressTime() {
		return suppressTime;
	}

	public boolean isSuppressed() {
		return suppressTime > 0;
	}

	public void setSuppressTime(int time) {
		this.suppressTime = Math.max(this.suppressTime, time);
	}

	public void decreaseSuppression() {
		suppressTime = Math.max(0, suppressTime - 1);
	}

	public void decreaseSuppression(int value) {
		suppressTime = Math.max(0, suppressTime - value);
	}

	public int getLockTime() {
		return lockTime;
	}

	public void setLockTime(int time) {
		this.lockTime = Math.max(this.lockTime, time);
	}

	public void addLockTime(int time) {
		lockTime += time;
	}

	public void decreaseLockTime() {
		lockTime = Math.max(0, lockTime - 1);
	}

	public void decreaseLockTime(int value) {
		lockTime = Math.max(0, lockTime - value);
	}

	public int getNullTime() {
		return nullTime;
	}

	public boolean isNullMode() {
		return nullTime > 0;
	}

	public void setNullTime(int time) {
		this.nullTime = Math.max(this.nullTime, time);
	}

	public void addNullTime(int time) {
		nullTime += time;
	}

	public void decreaseNullTime() {
		nullTime = Math.max(0, nullTime - 1);
	}

	public void decreaseNullTime(int value) {
		nullTime = Math.max(0, nullTime - value);
	}

	public int getBlindTime() {
		return blindTime;
	}

	public boolean isBlinded() {
		return blindTime > 0;
	}

	public void setBlindTime(int time) {
		this.blindTime = Math.max(this.blindTime, time);
		if (isBlinded()) {
			Collections.shuffle(cards);
		}
	}

	public void addBlindTime(int time) {
		blindTime += time;
		if (isBlinded()) {
			Collections.shuffle(cards);
		}
	}

	public void decreaseBlindTime() {
		blindTime = Math.max(0, blindTime - 1);
		if (isBlinded()) {
			Collections.shuffle(cards);
		}
	}

	public void decreaseBlindTime(int value) {
		blindTime = Math.max(0, blindTime - value);
		if (isBlinded()) {
			Collections.shuffle(cards);
		}
	}

	public int getHiddenMana() {
		return hideMana;
	}

	public boolean isHidingMana() {
		return hideMana > 0 || isNullMode();
	}

	public void setHiddenMana(int time) {
		this.hideMana = Math.max(this.hideMana, time);
	}

	public void addHiddenMana(int time) {
		hideMana += time;
	}

	public void decreaseHiddenMana() {
		hideMana = Math.max(0, hideMana - 1);
	}

	public void decreaseHiddenMana(int value) {
		hideMana = Math.max(0, hideMana - value);
	}

	public float getBaseHealingFac() {
		return 1 + (combo.getLeft() == Race.HUMAN ? 0.25f : 0);
	}

	public float getHealingFac() {
		float fac = healingFac * getBaseHealingFac();
		if (bleeding > 0) {
			fac *= 0.5;
		} else if (regeneration > 0) {
			fac *= 1.25;
		}

		return fac;
	}

	public void setHealingFac(float healingFac) {
		this.healingFac = healingFac;
	}

	public int getBleeding() {
		return bleeding;
	}

	public void setBleeding(int bleeding) {
		bleeding = Math.max(0, bleeding);

		if (regeneration > 0) {
			regeneration -= bleeding;
			if (regeneration < 0) {
				bleeding += regeneration;
				regeneration = 0;
			} else {
				bleeding = 0;
			}
		}

		this.bleeding = bleeding;
		if (this.bleeding > 9999) {
			this.bleeding = 0;
			this.hp = 0;
		}
	}

	public void addBleeding(int bleeding) {
		bleeding = Math.max(0, bleeding);

		if (regeneration > 0) {
			regeneration -= bleeding;
			if (regeneration < 0) {
				bleeding += regeneration;
				regeneration = 0;
			} else {
				bleeding = 0;
			}
		}

		this.bleeding += bleeding;
		if (this.bleeding > 9999) {
			this.bleeding = 0;
			this.hp = 0;
		}
	}

	public void decreaseBleeding(int value) {
		bleeding = Math.max(0, bleeding - value);
	}

	public void decreaseBleeding() {
		bleeding = Math.max(0, (int) (bleeding * 0.9));
	}

	public int getRegeneration() {
		return regeneration;
	}

	public void setRegeneration(int regeneration) {
		regeneration = Math.max(0, regeneration);

		if (bleeding > 0) {
			bleeding -= regeneration;
			if (bleeding < 0) {
				regeneration += bleeding;
				bleeding = 0;
			} else {
				regeneration = 0;
			}
		}

		this.regeneration = Math.min(regeneration, 9999);
	}

	public void addRegeneration(int regeneration) {
		regeneration = Math.max(0, regeneration);

		if (bleeding > 0) {
			bleeding -= regeneration;
			if (bleeding < 0) {
				regeneration += bleeding;
				bleeding = 0;
			} else {
				regeneration = 0;
			}
		}

		this.regeneration = Math.min(this.regeneration + regeneration, 9999);
	}

	public void decreaseRegeneration(int value) {
		regeneration = Math.max(0, regeneration - value);
	}

	public void decreaseRegeneration() {
		regeneration = Math.max(0, (int) (regeneration * 0.5));
	}

	protected void triggerEffect(EffectTrigger trigger) {
		Arena arena = game.getArena();
		if (arena != null) {
			List<SlotColumn> slots = arena.getSlots().get(side);
			game.applyPersistentEffects(trigger, side, -1);
			for (SlotColumn slt : slots) {
				if (slt.getTop() != null) {
					game.applyEffect(trigger, slt.getTop(), side, slt.getIndex());
				}
			}

			EffectTrigger other = switch (trigger) {
				case ON_HEAL -> ON_OP_HEAL;
				case ON_DAMAGE -> ON_OP_DAMAGE;
				case ON_DRAW -> ON_OP_DRAW;
				case ON_MANUAL_DRAW -> ON_OP_MANUAL_DRAW;
				default -> null;
			};

			if (other != null) {
				slots = arena.getSlots().get(side.getOther());
				game.applyPersistentEffects(other, side.getOther(), -1);
				for (SlotColumn slt : slots) {
					if (slt.getTop() != null) {
						game.applyEffect(other, slt.getTop(), side.getOther(), slt.getIndex());
					}
				}
			}
		}
	}

	public Hand getOponent() {
		return game.getHands().get(side.getOther());
	}

	public List<SlotColumn> getSlots() {
		return game.getArena().getSlots().get(side);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Hand hand = (Hand) o;
		return Objects.equals(game, hand.game) && Objects.equals(acc, hand.acc) && side == hand.side;
	}

	@Override
	public int hashCode() {
		return Objects.hash(game, acc, side);
	}
}
