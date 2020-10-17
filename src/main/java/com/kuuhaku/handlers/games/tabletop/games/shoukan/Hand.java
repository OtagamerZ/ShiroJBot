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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class Hand {
	private final User user;
	private final LinkedList<Drawable> deque;
	private final List<Drawable> cards = new ArrayList<>();
	private final Side side;
	private int mana = 0;
	private int hp = 5000;

	public Hand(User user, List<Drawable> deque, Side side) {
		Collections.shuffle(deque);

		this.user = user;
		this.deque = new LinkedList<>(deque);
		this.side = side;

		redrawHand();
	}

	public void draw() {
		try {
			cards.add(deque.removeFirst().copy());
		} catch (NoSuchElementException ignore) {
		}
	}

	public void draw(Card card) {
		try {
			Drawable dr = deque.stream().filter(c -> c.getCard().equals(card)).findFirst().orElseThrow().copy();
			deque.remove(dr);
			cards.add(dr);
		} catch (NoSuchElementException ignore) {
		}
	}

	public void drawEquipment() {
		try {
			Drawable dr = deque.stream().filter(c -> c instanceof Equipment).findFirst().orElseThrow().copy();
			deque.remove(dr);
			cards.add(dr);
		} catch (NoSuchElementException ignore) {
		}
	}

	public void drawRace(Race race) {
		try {
			Drawable dr = deque.stream().filter(c -> c instanceof Champion && ((Champion) c).getRace() == race).findFirst().orElseThrow().copy();
			deque.remove(dr);
			cards.add(dr);
		} catch (NoSuchElementException ignore) {
		}
	}

	public void redrawHand() {
		for (int i = 0; i < 5; i++) draw();
	}

	public User getUser() {
		return user;
	}

	public LinkedList<Drawable> getDeque() {
		return deque;
	}

	public List<Drawable> getCards() {
		return cards;
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

		user.openPrivateChannel().complete()
				.sendMessage("Escolha uma carta para jogar (digite a posição da carta na mão, no campo e se ela é virada para baixo (`s`) ou não (`n`). Ex: `0,0,n`), mude a postura de uma carta (digite apenas a posição da carta no campo) ou use os botões na mensagem enviada para render-se, comprar uma carta ou mudar de turno.")
				.addFile(Helper.getBytes(bi, "png"), "hand.png")
				.queue();
	}

	public int getMana() {
		return mana;
	}

	public void addMana(int value) {
		mana += value;
	}

	public void removeMana(int value) {
		mana = Math.max(0, mana - value);
	}

	public int getHp() {
		return hp;
	}

	public void addHp(int value) {
		hp += value;
	}

	public void removeHp(int value) {
		hp = Math.max(0, hp - value);
	}
}
