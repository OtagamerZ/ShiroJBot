/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Hand {
	private final Shoukan game;
	private final User user;
	private final LinkedList<Drawable> deque;
	private final List<Drawable> cards = new ArrayList<>();
	private final List<Drawable> destinyDeck = new ArrayList<>();
	private final Side side;
	private final int startingCount;
	private final int manaPerTurn;
	private int mana;
	private int hp;
	private int suppressTime = 0;
	private int lockTime = 0;
	private int manaReturn = 0;

	public Hand(Shoukan game, User user, Kawaipon kp, Side side) {
		deque = new LinkedList<>() {{
			addAll(kp.getChampions());
		}};
		deque.sort(Comparator
				.comparing(d -> ((Champion) d).getMana()).reversed()
				.thenComparing(c -> ((Champion) c).getCard().getName(), String.CASE_INSENSITIVE_ORDER)
		);
		if (kp.getDestinyDraw() != null) {
			for (int i : kp.getDestinyDraw()) {
				if (i > deque.size())
					destinyDeck.add(deque.get(Helper.rng(deque.size(), true)));
				else
					destinyDeck.add(deque.get(i));
			}
		}
		for (Drawable drawable : destinyDeck) {
			deque.remove(drawable);
		}
		deque.addAll(kp.getEquipments());
		deque.addAll(kp.getFields());

		this.user = user;
		this.side = side;
		this.game = game;

		if (game.getCustom() != null) {
			mana = Helper.minMax(game.getCustom().optInt("mana", 0), 0, 20);
			hp = Helper.minMax(game.getCustom().optInt("hp", 5000), 500, 25000);
			startingCount = Helper.minMax(game.getCustom().optInt("cartasini", 5), 1, 10);
			manaPerTurn = Helper.minMax(game.getCustom().optInt("manapt", 5), 1, 20);

			if (game.getCustom().optBoolean("semequip"))
				getDeque().removeIf(d -> d instanceof Equipment);
			if (game.getCustom().optBoolean("semfield"))
				getDeque().removeIf(d -> d instanceof Field);

			switch (game.getCustom().optString("arcade")) {
				case "roleta" -> {
					deque.removeIf(d -> d instanceof Champion);
					deque.addAll(Collections.nCopies(30, CardDAO.getChampion("AKAME")));
				}
				case "blackrock" -> {
					Field f = CardDAO.getField("OTHERWORLD");
					assert f != null;
					f.setAcc(AccountDAO.getAccount(user.getId()));
					game.getArena().setField(f);
					this.deque.removeIf(d -> d instanceof Champion || d instanceof Field);
					for (String name : new String[]{"MATO_KUROI", "SAYA_IRINO", "YOMI_TAKANASHI", "YUU_KOUTARI", "TAKU_KATSUCHI", "KAGARI_IZURIHA"}) {
						Champion c = CardDAO.getChampion(name);
						deque.addAll(Collections.nCopies(6, c));
					}
				}
				case "instakill" -> hp = 1;
			}
		} else {
			mana = 0;
			hp = 5000;
			startingCount = 5;
			manaPerTurn = 5;
		}

		Collections.shuffle(deque);
		redrawHand();
	}

	public boolean manualDraw() {
		try {
			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() == 4 && getDeque().stream().anyMatch(d -> d instanceof Champion))
				manualDrawChampion();
			else cards.add(getDeque().removeFirst().copy());
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public void destinyDraw() {
		if (destinyDeck.size() > 0) {
			cards.add(destinyDeck.remove(Helper.rng(destinyDeck.size(), true)).copy());
			deque.addAll(destinyDeck);
			destinyDeck.clear();
		}
	}

	public void draw() {
		if (lockTime > 0) return;
		try {
			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() == 4 && getDeque().stream().anyMatch(d -> d instanceof Champion))
				drawChampion();
			else cards.add(getDeque().removeFirst().copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void draw(Card card) {
		if (lockTime > 0) return;
		try {
			Drawable dr = getDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void draw(Drawable drawable) {
		if (lockTime > 0) return;
		Card card = drawable.getCard();
		try {
			Drawable dr = getDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void drawChampion() {
		if (lockTime > 0) return;
		try {
			Drawable dr = getDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void manualDrawChampion() {
		try {
			Drawable dr = getDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void drawEquipment() {
		if (lockTime > 0) return;
		try {
			Drawable dr = getDeque().stream().filter(c -> c instanceof Equipment).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void drawField() {
		if (lockTime > 0) return;
		try {
			Drawable dr = getDeque().stream().filter(c -> c instanceof Field).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void drawRace(Race race) {
		if (lockTime > 0) return;
		try {
			Drawable dr = getDeque().stream().filter(c -> c instanceof Champion && ((Champion) c).getRace() == race).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void redrawHand() {
		for (int i = 0; i < startingCount; i++) manualDraw();
	}

	public User getUser() {
		return user;
	}

	public LinkedList<Drawable> getDeque() {
		if (deque.size() == 0) {
			deque.addAll(destinyDeck);
			destinyDeck.clear();
		}
		return deque;
	}

	public List<Drawable> getCards() {
		return cards;
	}

	public List<Drawable> getAvailableCards() {
		return cards.stream().filter(Drawable::isAvailable).collect(Collectors.toList());
	}

	public List<Drawable> getDestinyDeck() {
		return destinyDeck;
	}

	public Side getSide() {
		return side;
	}

	public void showHand() {
		BufferedImage bi = new BufferedImage(Math.max(5, cards.size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = new ArrayList<>(getCards());
		Account acc = AccountDAO.getAccount(user.getId());

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(acc, false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			if (cards.get(i).isAvailable())
				Profile.printCenteredString(String.valueOf(i + 1), 225, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 90, g2d);
		}

		g2d.dispose();

		user.openPrivateChannel()
				.flatMap(c -> c.sendMessage("Escolha uma carta para jogar (digite a posição da carta na mão, no campo e se ela posicionada em modo de ataque (`A`), defesa (`D`) ou virada para baixo (`B`). Ex: `0,0,a`), mude a postura de uma carta (digite apenas a posição da carta no campo) ou use os botões na mensagem enviada para avançar o turno, comprar uma carta ou render-se.")
						.addFile(Helper.getBytes(bi, "png"), "hand.png")
				)
				.queue(null, Helper::doNothing);
	}

	public void showEnemyHand() {
		Hand enemy = game.getHands().get(side == Side.TOP ? Side.BOTTOM : Side.TOP);
		BufferedImage bi = new BufferedImage(Math.max(5, enemy.getCards().size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = new ArrayList<>(enemy.getCards());
		Account acc = AccountDAO.getAccount(enemy.getUser().getId());

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(acc, false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			try {
				BufferedImage so = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/shinigami_overlay.png")));
				g2d.drawImage(so, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			} catch (IOException ignore) {
			}
		}

		g2d.dispose();

		user.openPrivateChannel()
				.flatMap(c -> c.sendMessage("Visualizando as cartas na mão inimiga.")
						.addFile(Helper.getBytes(bi, "png"), "hand.png")
				)
				.queue(null, Helper::doNothing);
	}

	public int sumAttack() {
		return cards.stream().filter(d -> d instanceof Champion).mapToInt(d -> ((Champion) d).getAtk()).sum();
	}

	public int getMana() {
		return mana;
	}

	public int getManaPerTurn() {
		return isSuppressed() ? 0 : manaPerTurn;
	}

	public void setMana(int value) {
		mana = value;
	}

	public void addMana(int value) {
		mana += isSuppressed() ? 0 : value;
	}

	public void removeMana(int value) {
		mana = Math.max(0, mana - value);
	}

	public int getHp() {
		return hp;
	}

	public void setHp(int value) {
		hp = value;
	}

	public void addHp(int value) {
		hp += value;
	}

	public void removeHp(int value) {
		hp = Math.max(0, hp - value);
	}

	public void crippleHp(int value) {
		hp = Math.max(1, hp - value);
	}

	public boolean isSuppressed() {
		return suppressTime > 0;
	}

	public void setSuppressTime(int time) {
		this.suppressTime = time;
		this.manaReturn += mana;
		this.mana = 0;
	}

	public void decreaseSuppression() {
		suppressTime = Math.max(0, suppressTime - 1);
		if (suppressTime == 0) {
			mana += manaReturn;
			manaReturn = 0;
		}
	}

	public void addLockTime(int time) {
		lockTime += time;
	}

	public void decreaseLockTime() {
		lockTime = Math.max(0, lockTime - 1);
	}

	public int getLockTime() {
		return lockTime;
	}
}
