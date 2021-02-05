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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.handlers.games.tabletop.utils.InfiniteList;
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

public class TeamHand extends Hand {
	private final Shoukan game;
	private final InfiniteList<User> users = new InfiniteList<>();
	private final InfiniteList<LinkedList<Drawable>> deques = new InfiniteList<>();
	private final InfiniteList<List<Drawable>> cards = new InfiniteList<>();
	private final InfiniteList<List<Drawable>> destinyDecks = new InfiniteList<>();
	private final Side side;
	private final int startingCount;
	private final int manaPerTurn;
	private int mana;
	private int hp;
	private int suppressTime = 0;
	private int lockTime = 0;
	private int manaReturn = 0;

	public TeamHand(Shoukan game, List<User> users, List<Kawaipon> kps, Side side) {
		super(game, null, kps.get(0), null);
		this.side = side;
		this.game = game;
		if (game.getCustom() != null) {
			mana = Helper.clamp(game.getCustom().optInt("mana", 0), 0, 20);
			hp = Helper.clamp(game.getCustom().optInt("hp", 5000), 500, 25000);
			startingCount = Helper.clamp(game.getCustom().optInt("cartasini", 5), 1, 10);
			manaPerTurn = Helper.clamp(game.getCustom().optInt("manapt", 5), 1, 20);
		} else {
			mana = 0;
			hp = 5000;
			startingCount = 5;
			manaPerTurn = 5;
		}

		for (int i = 0; i < users.size(); i++) {
			Kawaipon kp = kps.get(i);
			User user = users.get(i);

			LinkedList<Drawable> deque = new LinkedList<>() {{
				addAll(kp.getChampions());
			}};
			List<Drawable> destinyDeck = new ArrayList<>();

			deque.sort(Comparator
					.comparing(d -> ((Champion) d).getMana()).reversed()
					.thenComparing(c -> ((Champion) c).getCard().getName(), String.CASE_INSENSITIVE_ORDER)
			);
			deque.addAll(kp.getEquipments());
			deque.addAll(kp.getFields());

			Account acc = AccountDAO.getAccount(user.getId());
			for (Drawable d : deque) d.setAcc(acc);

			this.users.add(user);

			if (game.getCustom() != null) {
				if (game.getCustom().optBoolean("semequip"))
					deque.removeIf(d -> d instanceof Equipment);
				if (game.getCustom().optBoolean("semfield"))
					deque.removeIf(d -> d instanceof Field);

				switch (game.getCustom().optString("arcade")) {
					case "roleta" -> {
						for (Drawable d : deque) {
							if (d instanceof Champion) {
								Champion c = (Champion) d;
								c.setRawEffect("""
										if (ep.getTrigger() == EffectTrigger.ON_ATTACK) {
											int rng = Math.round(Math.random() * 100);
											if (rng < 25) {
												Hand h = ep.getHands().get(ep.getSide());
												h.setHp(h.getHp() / 2);
											} else if (rng < 50) {
												Hand h = ep.getHands().get(ep.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);
												h.setHp(h.getHp() / 2);
											}
										}
										%s
										""".formatted(Helper.getOr(c.getRawEffect(), "")));
							}
						}
					}
					case "blackrock" -> {
						Field f = CardDAO.getField("OTHERWORLD");
						assert f != null;
						f.setAcc(AccountDAO.getAccount(user.getId()));
						game.getArena().setField(f);
						deque.removeIf(d -> d instanceof Champion || d instanceof Field);
						for (String name : new String[]{"MATO_KUROI", "SAYA_IRINO", "YOMI_TAKANASHI", "YUU_KOUTARI", "TAKU_KATSUCHI", "KAGARI_IZURIHA"}) {
							Champion c = CardDAO.getChampion(name);
							deque.addAll(Collections.nCopies(6, c));
						}
						for (Drawable d : deque) d.setAcc(acc);
					}
					case "instakill" -> {
						deque.removeIf(d -> d instanceof Equipment && ((Equipment) d).getCharm() != null && ((Equipment) d).getCharm() == Charm.SPELL);
						hp = 1;
					}
				}
			}

			if (kp.getDestinyDraw() != null) {
				int champs = kp.getChampions().size();
				for (int x : kp.getDestinyDraw()) {
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

			this.deques.add(deque);
			this.destinyDecks.add(destinyDeck);
			this.cards.add(new ArrayList<>());
		}

		for (int i = 0; i < this.users.size(); i++, next()) {
			redrawHand();
		}
	}

	public boolean manualDraw() {
		try {
			List<Drawable> cards = getCards();

			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() == 4 && getDeque().stream().anyMatch(d -> d instanceof Champion))
				manualDrawChampion();
			else cards.add(getDeque().removeFirst().copy());
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public Drawable destinyDraw() {
		LinkedList<Drawable> deque = getDeque();
		List<Drawable> destinyDeck = destinyDecks.getCurrent();
		List<Drawable> cards = getCards();

		if (destinyDeck.size() > 0) {
			Drawable dr = destinyDeck.remove(Helper.rng(destinyDeck.size(), true));
			cards.add(dr.copy());
			deque.addAll(destinyDeck);
			destinyDeck.clear();
			return dr;
		}
		return null;
	}

	public Drawable draw() {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr;
			if (cards.stream().filter(d -> d instanceof Equipment || d instanceof Field).count() == 4 && getDeque().stream().anyMatch(d -> d instanceof Champion))
				dr = drawChampion();
			else {
				dr = getDeque().removeFirst();
				cards.add(dr.copy());
			}
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(Card card) {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable draw(Drawable drawable) {
		if (lockTime > 0) return null;
		Card card = drawable.getCard();
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawChampion() {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public void manualDrawChampion() {
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c instanceof Champion).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public Drawable drawEquipment() {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c instanceof Equipment).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawSpell() {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c instanceof Equipment && ((Equipment) c).getCharm() == Charm.SPELL).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawHighest(boolean attack) {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c instanceof Equipment).max(Comparator.comparingInt(c -> attack ? ((Equipment) c).getAtk() : ((Equipment) c).getDef())).orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawField() {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c instanceof Field).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public Drawable drawRace(Race race) {
		if (lockTime > 0) return null;
		try {
			List<Drawable> cards = getCards();

			Drawable dr = getDeque().stream().filter(c -> c instanceof Champion && ((Champion) c).getRace() == race).findFirst().orElseThrow();
			getDeque().remove(dr);
			cards.add(dr.copy());
			return dr;
		} catch (NoSuchElementException ignore) {
			return null;
		}
	}

	public void redrawHand() {
		LinkedList<Drawable> deque = getDeque();
		List<Drawable> cards = getCards();

		deque.addAll(cards);
		cards.clear();
		Collections.shuffle(deque);
		for (int i = 0; i < startingCount; i++) manualDraw();
	}

	public void next() {
		users.getNext();
		deques.getNext();
		cards.getNext();
		destinyDecks.getNext();
	}

	public User getUser() {
		return users.getCurrent();
	}

	public LinkedList<Drawable> getDeque() {
		LinkedList<Drawable> deque = deques.getCurrent();
		List<Drawable> destinyDeck = getDestinyDeck();

		if (deque.size() == 0) {
			deque.addAll(destinyDeck);
			destinyDeck.clear();
		}
		return deque;
	}

	public List<Drawable> getCards() {
		return cards.getCurrent();
	}

	public List<Drawable> getAvailableCards() {
		return cards.getCurrent().stream()
				.filter(Drawable::isAvailable)
				.collect(Collectors.toList());
	}

	public List<Drawable> getDestinyDeck() {
		return destinyDecks.getCurrent();
	}

	public Side getSide() {
		return side;
	}

	public void showHand() {
		List<Drawable> cards = getCards();
		BufferedImage bi = new BufferedImage(Math.max(5, cards.size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 90));

		for (int i = 0; i < cards.size(); i++) {
			g2d.drawImage(cards.get(i).drawCard(false), bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 100, null);
			if (cards.get(i).isAvailable())
				Profile.printCenteredString(String.valueOf(i + 1), 225, bi.getWidth() / (cards.size() + 1) * (i + 1) - (225 / 2), 90, g2d);
		}

		g2d.dispose();

		getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage("Escolha uma carta para jogar (digite a posição da carta na mão, no campo e se ela posicionada em modo de ataque (`A`), defesa (`D`) ou virada para baixo (`B`). Ex: `0,0,a`), mude a postura de uma carta (digite apenas a posição da carta no campo) ou use os botões na mensagem enviada para avançar o turno, comprar uma carta ou render-se.")
						.addFile(Helper.getBytes(bi, "png"), "hand.png")
				)
				.queue(null, Helper::doNothing);
	}

	public void showEnemyHand() {
		TeamHand op = (TeamHand) game.getHands().get(side == Side.TOP ? Side.BOTTOM : Side.TOP);
		BufferedImage bi = new BufferedImage(Math.max(5, op.getCards().size()) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 90));

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
						.addFile(Helper.getBytes(bi, "png"), "hand.png")
				)
				.queue(null, Helper::doNothing);
	}

	public void showEnemyDeck(int amount) {
		TeamHand op = (TeamHand) game.getHands().get(side == Side.TOP ? Side.BOTTOM : Side.TOP);
		BufferedImage bi = new BufferedImage(Math.max(5, amount) * 300, 450, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 90));

		List<Drawable> cards = op.getDeque();

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
						.addFile(Helper.getBytes(bi, "png"), "hand.png")
				)
				.queue(null, Helper::doNothing);
	}

	public int sumAttack() {
		return getCards().stream().filter(d -> d instanceof Champion).mapToInt(d -> ((Champion) d).getAtk()).sum();
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

	public String getMentions() {
		return getUser().getAsMention() + " e " + users.peekNext().getAsMention();
	}

	public List<String> getNames() {
		return List.of(getUser().getName(), users.peekNext().getName());
	}
}
