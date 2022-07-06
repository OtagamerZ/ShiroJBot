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

package com.kuuhaku.interfaces.shoukan;

import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public interface Drawable<T extends Drawable<T>> extends Cloneable {
	int MAX_NAME_LENGTH = 15;
	int MAX_DESC_LENGTH = 210;

	String getId();

	Card getCard();

	default Card getVanity() {
		return getCard();
	}

	default SlotColumn getSlot() {
		return new SlotColumn(getHand().getSide(), -1);
	}

	default void setSlot(SlotColumn slot) {
	}

	Hand getHand();

	void setHand(Hand hand);

	default String getDescription(I18N locale) {
		return "";
	}

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

	default double getPower() {
		return 1;
	}

	boolean isSolid();

	void setSolid(boolean solid);

	boolean isAvailable();

	void setAvailable(boolean available);

	default boolean isFlipped() {
		return false;
	}

	default void setFlipped(boolean flipped) {

	}

	void reset();

	BufferedImage render(I18N locale, Deck deck);

	default void drawCosts(Graphics2D g2d) {
		BufferedImage icon;
		int y = 55;

		g2d.setFont(Fonts.STAATLICHES.deriveFont(Font.BOLD, 20));
		FontMetrics m = g2d.getFontMetrics();

		if (getMPCost() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/mana.png");
			assert icon != null;
			int x = 200 - icon.getWidth();

			String val = String.valueOf(getMPCost());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.CYAN);
			Graph.drawOutlinedString(g2d, val, x - m.stringWidth(val) - 2, y - 4 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y += icon.getHeight() + 5;
		}

		if (getHPCost() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/blood.png");
			assert icon != null;
			int x = 200 - icon.getWidth();

			String val = String.valueOf(getHPCost());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.RED);
			Graph.drawOutlinedString(g2d, val, x - m.stringWidth(val) - 2, y - 4 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
		}
	}

	default void drawAttributes(Graphics2D g2d, boolean desc) {
		BufferedImage icon;
		int y = desc ? 225 : 279;

		g2d.setFont(Fonts.STAATLICHES.deriveFont(Font.BOLD, 20));
		FontMetrics m = g2d.getFontMetrics();

		if (getDef() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/defense.png");
			assert icon != null;
			int x = 25;

			String val = String.valueOf(getDef());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.GREEN);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y -= icon.getHeight() + 5;
		}

		if (getDmg() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/attack.png");
			assert icon != null;
			int x = 25;

			String val = String.valueOf(getDmg());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.RED);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y -= icon.getHeight() + 5;
		}

		if (getBlock() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/block.png");
			assert icon != null;
			int x = 25;

			String val = getBlock() + "%";
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.GRAY);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
			y -= icon.getHeight() + 5;
		}

		if (getDodge() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/dodge.png");
			assert icon != null;
			int x = 25;

			String val = getDodge() + "%";
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.ORANGE);
			Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, 2, Color.BLACK);
		}
	}

	T clone() throws CloneNotSupportedException;

	@SuppressWarnings("unchecked")
	default T copy() {
		try {
			T clone = clone();
			clone.reset();

			return clone;
		} catch (CloneNotSupportedException e) {
			return (T) this;
		}
	}

	default T withCopy(Consumer<T> act) {
		T t = copy();
		act.accept(t);
		return t;
	}
}
