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

package com.kuuhaku.handlers.games.tabletop.games.hitotsu;

import com.kuuhaku.controller.postgresql.RarityColorsDAO;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.RarityColors;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class Hand {
	private final User user;
	private final List<KawaiponCard> cards = new ArrayList<>();

	public Hand(User user, GameDeque<KawaiponCard> deque) {
		this.user = user;
		redrawHand(deque);
	}

	public void draw(GameDeque<KawaiponCard> deque) throws NoSuchElementException {
		cards.add(deque.removeFirst());
	}

	public void redrawHand(GameDeque<KawaiponCard> deque) {
		for (int i = 0; i < 5; i++) draw(deque);
	}

	public User getUser() {
		return user;
	}

	public List<KawaiponCard> getCards() {
		return cards;
	}

	public void setCards(List<KawaiponCard> cards) {
		this.cards.clear();
		this.cards.addAll(cards);
	}

	public void showHand() {
		BufferedImage bi = new BufferedImage(1000, 650, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 50));

		List<KawaiponCard> cards = new ArrayList<>(getCards());
		g2d.translate(bi.getWidth() / 2f - 104, 75);

		for (int i = 0; i < cards.size(); i++) {
			KawaiponCard kc = cards.get(i);
			BufferedImage card = kc.getCard().drawCard(kc.isFoil());

			Helper.drawRotated(g2d, card, 128, 450, -90 + (180f / (cards.size() + 1) * (i + 1)));

			RarityColors rc = RarityColorsDAO.getColor(kc.getCard().getRarity());

			if (kc.isFoil()) g2d.setColor(rc.getPrimary().brighter());
			else g2d.setColor(rc.getPrimary());

			Helper.writeRotated(g2d, kc.isFoil() ? "*" + i + "*" : String.valueOf(i), 128, 450, -90 + (180f / (cards.size() + 1) * (i + 1)));
		}

		g2d.dispose();

		user.openPrivateChannel()
				.flatMap(c -> c.sendMessage("Escolha uma carta que seja do mesmo anime ou raridade da ultima jogada no monte, clique em \uD83D\uDCCB para ver detalhes sobre as cartas ou clique em \uD83D\uDCE4 para comprar uma carta e passar a vez.")
						.addFile(Helper.getBytes(bi, "png"), "hand.png"))
				.queue(null, Helper::doNothing);
	}
}
