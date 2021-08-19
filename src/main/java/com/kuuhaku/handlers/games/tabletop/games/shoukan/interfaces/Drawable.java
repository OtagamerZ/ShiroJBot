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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hand;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface Drawable {
	BufferedImage drawCard(boolean flipped);

	Card getCard();

	boolean isFlipped();

	void setFlipped(boolean flipped);

	boolean isAvailable();

	void setAvailable(boolean available);

	Shoukan getGame();

	void setGame(Shoukan game);

	Account getAcc();

	void setAcc(Account acc);

	void bind(Hand h);

	Drawable copy();

	static void drawAttributes(BufferedImage in, int atk, int def) {
		Graphics2D g2d = in.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));

		if (atk > 0) {
			BufferedImage icon = Helper.getResourceAsImage(Drawable.class, "shoukan/attack.png");
			g2d.drawImage(icon, 19, 232, null);

			g2d.setColor(Color.red);
			Profile.drawOutlinedText(String.valueOf(atk), 45, 250, g2d);
		}

		if (def > 0) {
			BufferedImage icon = Helper.getResourceAsImage(Drawable.class, "shoukan/defense.png");
			g2d.drawImage(icon, 182, 232, null);

			g2d.setColor(Color.green);
			Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 250, g2d);
		}

		g2d.dispose();
	}

	static void drawEvoAttributes(BufferedImage in, int atk, int def) {
		Graphics2D g2d = in.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));

		if (atk > 0) {
			BufferedImage icon = Helper.getResourceAsImage(Drawable.class, "shoukan/attack.png");
			g2d.drawImage(icon, 19, 298, null);

			g2d.setColor(Color.red);
			Profile.drawOutlinedText(String.valueOf(atk), 45, 316, g2d);
		}

		if (def > 0) {
			BufferedImage icon = Helper.getResourceAsImage(Drawable.class, "shoukan/defense.png");
			g2d.drawImage(icon, 182, 298, null);

			g2d.setColor(Color.green);
			Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 316, g2d);
		}

		g2d.dispose();
	}
}
