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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.ArcadeMode;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.BondedList;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.InfiniteList;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.*;

public class TeamHand extends Hand {
	private final Map<Race, Long> raceCount;
	private final Pair<Race, Race> combo;
	private final InfiniteList<String> users = new InfiniteList<>();
	private final InfiniteList<Account> accs = new InfiniteList<>();
	private final InfiniteList<BondedList<Drawable>> decks = new InfiniteList<>();
	private final InfiniteList<BondedList<Drawable>> cards = new InfiniteList<>();
	private final InfiniteList<BondedList<Drawable>> destinyDecks = new InfiniteList<>();
	private final InfiniteList<Hero> heroes = new InfiniteList<>();
	private final double divergence;
	private final double avgCost;
	private float healingFac = 1;

	public TeamHand(Shoukan game, List<User> users, List<Deck> dks, Side side) {
		super(game, side);

		this.divergence = dks.stream().mapToDouble(Deck::getAverageDivergence).average().orElse(0);
		this.avgCost = dks.stream().mapToDouble(Deck::getAverageCost).average().orElse(0);
		raceCount = dks.stream()
				.flatMap(kp -> kp.getChampions().stream())
				.collect(Collectors.groupingBy(Champion::getRace, Collectors.counting()));

		combo = Race.getCombo(dks.stream()
				.flatMap(kp -> kp.getChampions().stream())
				.collect(Collectors.toList()));
		setMitigation(combo.getRight() == Race.HUMAN
				? dks.stream()
				.map(Deck::getChampions)
				.flatMap(List::stream)
				.filter(c -> c.getMana() <= 2)
				.count() * 0.005f : 0
		);

		for (int i = 0; i < users.size(); i++) {
			Deck dk = dks.get(i);
			User user = users.get(i);
			game.getDivergence().put(user.getId(), dk.getAverageDivergence());

			Hero hero = KawaiponDAO.getHero(user.getId());
			if (hero == null || hero.getQuest() != null) {
				hero = null;
			}

			Account acc = AccountDAO.getAccount(user.getId());
			this.users.add(user.getId());
			this.accs.add(acc);
			this.heroes.add(hero);

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
			BondedList<Drawable> deque = Stream.of(dk.getChampions(), dk.getEquipments(), dk.getFields())
					.flatMap(List::stream)
					.map(Drawable::copy)
					.collect(Collectors.toCollection(() -> new BondedList<>(bonding)));
			if (hero != null) deque.add(hero.toChampion());

			BondedList<Drawable> destinyDeck = new BondedList<>(bonding);

			if (game.getRules().noEquip())
				deque.removeIf(d -> d instanceof Equipment e && !e.isSpell());
			if (game.getRules().noSpell())
				deque.removeIf(d -> d instanceof Equipment e && e.isSpell());
			if (game.getRules().noField())
				deque.removeIf(d -> d instanceof Field);

			switch (game.getRules().arcade()) {
				case ROULETTE -> {
					for (Drawable d : deque) {
						if (d instanceof Champion c) {
							c.setRawEffect("""
																	%s
									if (ep.getTrigger() == EffectTrigger.ON_ATTACK) {
										int rng = Math.round(Math.random() * 100);
										if (rng < 25) {
											Hand h = ep.getHands().get(ep.getSide());
											h.setHp(h.getHp() / 2);
										} else if (rng < 50) {
											Hand h = ep.getHands().get(ep.getSide().getOther());
											h.setHp(h.getHp() / 2);
										}
									}
									""".formatted(Helper.getOr(c.getRawEffect(), "")));
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
				case INSTAKILL -> deque.removeIf(d -> d instanceof Equipment e && e.hasEffect());
				case CARDMASTER -> {
					deque.clear();
					deque.addAll(CardDAO.getAllChampions(false));
					deque.addAll(CardDAO.getAllAvailableEquipments());
					deque.addAll(CardDAO.getAllAvailableFields());
				}
			}

			if (dk.getDestinyDraw() != null) {
				int champs = dk.getChampions().size();
				for (int x : dk.getDestinyDraw()) {
					if (x > champs) {
						destinyDeck.clear();
						break;
					} else
						destinyDeck.add(deque.get(x));
				}
			}
			for (Drawable drawable : destinyDeck) {
				deque.remove(drawable);
			}

			this.decks.add(deque);
			this.destinyDecks.add(destinyDeck);
			this.cards.add(new BondedList<>(bonding));
		}

		int baseHp = game.getRules().baseHp();
		int baseManaPerTurn = game.getRules().baseManaPerTurn();
		int maxCards = game.getRules().maxCards();

		setMana(game.getRules().mana());

		if (game.getRules().arcade() == ArcadeMode.INSTAKILL) {
			baseHp = 1;
		}

		int hpMod = combo.getLeft() == Race.DEMON ? -1500 : 0;
		int manaMod = combo.getLeft() == Race.ELF ? 1 : 0;

		setBaseHp(Math.max(baseHp + hpMod, 1));
		setHp(getBaseHp());

		setBaseManaPerTurn(Math.max(baseManaPerTurn + manaMod, 0));
		setManaPerTurn(getBaseManaPerTurn());

		setMaxCards(Math.max(maxCards
						+ (combo.getLeft() == Race.CREATURE ? 2 : 0)
						+ (combo.getRight() == Race.CREATURE ? 1 : 0)
				, 1)
		);

		for (int i = 0; i < this.users.size(); i++, next()) {
			redrawHand();
		}
	}

	public boolean manualDraw() {
		try {
			List<Drawable> cards = getCards();

			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() == 4 && getRealDeque().stream().anyMatch(d -> d instanceof Champion))
				manualDrawChampion();
			else {
				Drawable dr = getRealDeque().removeFirst().copy();
				cards.add(dr);

				if (dr instanceof Equipment e) {
					if (e.isSpell() && combo.getLeft() == Race.MYSTICAL)
						addMana(1);
					else if (!e.isSpell() && combo.getLeft() == Race.MACHINE)
						addHp(250);
				}
			}

			triggerEffect(ON_DRAW);
			triggerEffect(ON_MANUAL_DRAW);

			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public void destinyDraw() {
		LinkedList<Drawable> deque = getRealDeque();
		List<Drawable> destinyDeck = getDestinyDeck();
		List<Drawable> cards = getCards();

		if (destinyDeck.size() > 0) {
			Drawable dr = Helper.getRandomEntry(destinyDeck);
			destinyDeck.remove(dr);
			cards.add(dr.copy());
			deque.addAll(destinyDeck);
			destinyDeck.clear();
		}

		triggerEffect(ON_DRAW);
	}

	public Drawable draw() {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr;
			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() == 4 && getRealDeque().stream().anyMatch(d -> d instanceof Champion))
				dr = drawChampion();
			else {
				dr = getRealDeque().removeFirst();
				cards.add(dr.copy());

				if (dr instanceof Equipment e) {
					if (e.isSpell() && combo.getLeft() == Race.MYSTICAL)
						addMana(1);
					else if (!e.isSpell() && combo.getLeft() == Race.MACHINE)
						addHp(250);
				}
			}

			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(Card card) {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());

			if (dr instanceof Equipment e) {
				if (e.isSpell() && combo.getLeft() == Race.MYSTICAL)
					addMana(1);
				else if (!e.isSpell() && combo.getLeft() == Race.MACHINE)
					addHp(250);
			}

			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(Drawable drawable) {
		if (getLockTime() > 0) return null;
		Card card = drawable.getCard();
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());

			if (dr instanceof Equipment e) {
				if (e.isSpell() && combo.getLeft() == Race.MYSTICAL)
					addMana(1);
				else if (!e.isSpell() && combo.getLeft() == Race.MACHINE)
					addHp(250);
			}

			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(String name) {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().filter(c -> c.getCard().getId().equals(name)).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());

			if (dr instanceof Equipment e) {
				if (e.isSpell() && combo.getLeft() == Race.MYSTICAL)
					addMana(1);
				else if (!e.isSpell() && combo.getLeft() == Race.MACHINE)
					addHp(250);
			}

			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawChampion() {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
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
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());
			triggerEffect(ON_DRAW);
			triggerEffect(ON_MANUAL_DRAW);
		} catch (NoSuchElementException ignore) {
		}
	}

	public Drawable drawEquipment() {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Equipment e && !e.isSpell()).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());

			if (combo.getLeft() == Race.MACHINE)
				addHp(250);

			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawSpell() {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().filter(c -> c instanceof Equipment e && e.isSpell()).findFirst().orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());

			if (combo.getLeft() == Race.MYSTICAL)
				addMana(1);

			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawHighest(boolean attack) {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getRealDeque().stream().max(Comparator.comparingInt(c -> attack ? ((Equipment) c).getAtk() : ((Equipment) c).getDef())).orElseThrow();
			getRealDeque().remove(dr);
			cards.add(dr.copy());

			if (combo.getLeft() == Race.MACHINE)
				addHp(250);
			else if (combo.getLeft() == Race.MYSTICAL)
				addMana(1);

			triggerEffect(ON_DRAW);

			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawField() {
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

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
		if (getLockTime() > 0) return null;
		try {
			List<Drawable> cards = getCards();

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
		LinkedList<Drawable> deque = getRealDeque();
		List<Drawable> cards = getCards();

		List<Drawable> notUsed = cards.stream().filter(Drawable::isAvailable).toList();
		deque.addAll(notUsed);
		cards.removeIf(Drawable::isAvailable);

		Collections.shuffle(deque);
		int toDraw = Math.max(0, getMaxCards() - cards.size());
		for (int i = 0; i < toDraw; i++) manualDraw();
	}

	public void next() {
		users.getNext();
		decks.getNext();
		cards.getNext();
		destinyDecks.getNext();
		heroes.getNext();
	}

	public User getUser() {
		return Main.getInfo().getUserByID(users.getCurrent());
	}

	public Account getAcc() {
		return accs.getCurrent();
	}

	public InfiniteList<User> getUsers() {
		return users.stream()
				.map(Main.getInfo()::getUserByID)
				.collect(Collectors.toCollection(InfiniteList::new));
	}

	public InfiniteList<Account> getAccs() {
		return accs;
	}

	public Map<Race, Long> getRaceCount() {
		return raceCount;
	}

	public Pair<Race, Race> getCombo() {
		return combo;
	}

	public double getDivergence() {
		return divergence;
	}

	public double getAvgCost() {
		return avgCost;
	}

	public BondedList<Drawable> getDeque() {
		if (getLockTime() > 0) return new BondedList<>(getRealDeque().getBonding());
		else return getRealDeque();
	}

	public BondedList<Drawable> getRealDeque() {
		BondedList<Drawable> deque = decks.getCurrent();
		BondedList<Drawable> destinyDeck = getDestinyDeck();

		if (deque.isEmpty()) {
			deque.addAll(destinyDeck);
			destinyDeck.clear();
		}
		return deque;
	}

	public BondedList<Drawable> getCards() {
		return cards.getCurrent();
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
		return destinyDecks.getCurrent();
	}

	public Hero getHero() {
		return heroes.getCurrent();
	}

	public BufferedImage render() {
		List<Drawable> cards = getCards();
		BufferedImage bi = new BufferedImage(Math.max(5, cards.size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
		}

		g2d.dispose();

		return bi;
	}

	public void showHand() {
		List<Drawable> cards = getCards();
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
				.flatMap(c -> c.sendMessage("Escolha uma carta para jogar (digite a posição da carta na mão, no campo e se ela posicionada em modo de ataque (`A`), defesa (`D`) ou virada para baixo (`B`). Ex: `0,0,a`), mude a postura de uma carta (digite apenas a posição da carta no campo) ou use os botões na mensagem enviada para avançar o turno, comprar uma carta ou render-se.")
						.addFile(Helper.writeAndGet(bi, "hand", "png"))
				)
				.queue(null, Helper::doNothing);
	}

	public void showEnemyHand() {
		TeamHand op = (TeamHand) getGame().getHands().get(getSide().getOther());
		BufferedImage bi = new BufferedImage(Math.max(5, op.getCards().size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = op.getCards();

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
				.flatMap(c -> c.sendMessage("Visualizando as cartas na mão do oponente.")
						.addFile(Helper.writeAndGet(bi, "hand", "png"))
				)
				.queue(null, Helper::doNothing);
	}

	public void showEnemyDeck(int amount) {
		TeamHand op = (TeamHand) getGame().getHands().get(getSide().getOther());
		BufferedImage bi = new BufferedImage(Math.max(5, amount) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = op.getRealDeque();

		for (int i = 0; i < amount; i++) {
			g2d.drawImage(cards.get(i).drawCard(false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			try {
				BufferedImage so = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/shinigami_overlay.png")));
				g2d.drawImage(so, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			} catch (IOException ignore) {
			}
		}

		g2d.dispose();

		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage("Visualizando as próximas " + amount + " cartas do oponente.")
						.addFile(Helper.writeAndGet(bi, "hand", "png"))
				)
				.queue(null, Helper::doNothing);
	}

	public int getCardCount() {
		return (int) getCards().stream()
				.filter(d -> !(d instanceof Equipment e) || !e.isEffectOnly())
				.count();
	}

	public int sumAttack() {
		return getCards().stream().filter(d -> d instanceof Champion).mapToInt(d -> ((Champion) d).getAtk()).sum();
	}

	public String getMentions() {
		return getUser().getAsMention() + " e " + getUsers().peekNext().getAsMention();
	}

	public List<String> getNames() {
		return List.of(getUser().getName(), getUsers().peekNext().getName());
	}

	@Override
	public float getBaseHealingFac() {
		return 1 + (combo.getLeft() == Race.HUMAN ? 0.25f : 0);
	}

	@Override
	public float getHealingFac() {
		float fac = healingFac * getBaseHealingFac();
		if (getBleeding() > 0) {
			fac *= 0.5;
		} else if (getRegeneration() > 0) {
			fac *= 1.25;
		}

		return fac;
	}

	@Override
	public void addHp(int value, boolean trigger) {
		if (value <= 0) return;

		setHp((int) (getHp() + value * getHealingFac()));
		decreaseBleeding((int) (value * 0.25));

		if (trigger) {
			triggerEffect(ON_HEAL);
		}
	}
}
