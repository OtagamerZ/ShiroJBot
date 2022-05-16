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

package com.kuuhaku.interfaces;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.utils.Graph;
import com.kuuhaku.utils.IO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

public interface Drawable {
	int MAX_NAME_LENGTH = 16;
	int MAX_DESC_LENGTH = 215;

	Card getCard();

	int getIndex();
	AtomicInteger getIndexRef();

	Side getSide();

	default int getMPCost() {
		return 0;
	}

	default int getHPCost() {
		return 0;
	}

	default int getDmg() {
		return 0;
	}

	default int getDef() {
		return 0;
	}

	default int getDodge() {
		return 0;
	}

	default int getBlock() {
		return 0;
	}

	boolean isSolid();
	void setSolid(boolean solid);

	boolean isFlipped();
	void setFlipped(boolean flipped);

	BufferedImage render(I18N locale, Deck deck);

	int renderHashCode(I18N locale);

	default void drawCosts(Graphics2D g2d) {
		BufferedImage icon;
		int y = 55;

		g2d.setFont(new Font("Arial", Font.BOLD, 18));
		FontMetrics m = g2d.getFontMetrics();

		if (getMPCost() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/mana.png");
			assert icon != null;
			int x = 200 - icon.getWidth();

			String val = String.valueOf(getMPCost());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.CYAN);
			Graph.drawOutlinedString(g2d, val, x - m.stringWidth(val) - 3, y - 3 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y += icon.getHeight() + 5;
		}

		if (getHPCost() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/blood.png");
			assert icon != null;
			int x = 200 - icon.getWidth();

			String val = String.valueOf(getHPCost());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.RED);
			Graph.drawOutlinedString(g2d, val, x - m.stringWidth(val) - 3, y - 3 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
		}
	}

	default void drawAttributes(Graphics2D g2d) {
		BufferedImage icon;
		int y = 225;

		g2d.setFont(new Font("Arial", Font.BOLD, 18));
		FontMetrics m = g2d.getFontMetrics();

		if (getDef() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/defense.png");
			assert icon != null;
			int x = 25;

			String val = String.valueOf(getDef());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.GREEN);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y -= icon.getHeight() + 5;
		}

		if (getDmg() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/attack.png");
			assert icon != null;
			int x = 25;

			String val = String.valueOf(getDmg());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.RED);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y -= icon.getHeight() + 5;
		}

		if (getBlock() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/block.png");
			assert icon != null;
			int x = 25;

			String val = getBlock() + "%";
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.GRAY);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y -= icon.getHeight() + 5;
		}

		if (getDodge() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/dodge.png");
			assert icon != null;
			int x = 25;

			String val = getDodge() + "%";
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.ORANGE);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
		}
	}
}
