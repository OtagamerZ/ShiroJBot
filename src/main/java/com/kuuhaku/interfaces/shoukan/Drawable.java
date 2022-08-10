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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.id.LocalizedId;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.shoukan.Source;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import org.apache.commons.collections4.set.ListOrderedSet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public interface Drawable<T extends Drawable<T>> extends Cloneable {
	int MAX_NAME_LENGTH = 14;
	int MAX_DESC_LENGTH = 210;
	Font FONT = Fonts.OPEN_SANS_EXTRABOLD.deriveFont(Font.BOLD, 20);
	int BORDER_WIDTH = 3;

	String getId();

	Card getCard();

	default Card getVanity() {
		return getCard();
	}

	default List<String> getTags() {
		return List.of();
	}

	default SlotColumn getSlot() {
		return new SlotColumn(getHand().getGame(), getHand().getSide(), -1);
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

	default int getSCCost() {
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

	default int getCooldown() {
		return 0;
	}

	default void setCooldown(int time) {
	}

	default ListOrderedSet<String> getCurses() {
		return new ListOrderedSet<>();
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

		g2d.setFont(FONT);
		FontMetrics m = g2d.getFontMetrics();

		int y = 55;
		if (getMPCost() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/mana.png");
			assert icon != null;
			int x = 200 - icon.getWidth();

			String val = String.valueOf(getMPCost());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(new Color(0x3F9EFF));
			Graph.drawOutlinedString(g2d, val, x - m.stringWidth(val) - 2, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
			y += icon.getHeight() + 5;
		}

		if (getHPCost() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/blood.png");
			assert icon != null;
			int x = 200 - icon.getWidth();

			String val = String.valueOf(getHPCost());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.RED);
			Graph.drawOutlinedString(g2d, val, x - m.stringWidth(val) - 2, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
			y += icon.getHeight() + 5;
		}

		if (getSCCost() > 0) {
			icon = IO.getResourceAsImage("shoukan/icons/sacrifice.png");
			assert icon != null;
			int x = 200 - icon.getWidth();

			String val = String.valueOf(getSCCost());
			g2d.drawImage(icon, x, y, null);
			g2d.setColor(Color.LIGHT_GRAY);
			Graph.drawOutlinedString(g2d, val, x - m.stringWidth(val) - 2, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
		}
	}

	default void drawAttributes(Graphics2D g2d, boolean desc) {
		BufferedImage icon;
		g2d.setFont(FONT);
		FontMetrics m = g2d.getFontMetrics();

		{ // LEFT
			int y = desc ? 225 : 291;
			if (getDef() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/defense.png");
				assert icon != null;
				int x = 25;

				String val = String.valueOf(getDef());
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.GREEN);
				Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				y -= icon.getHeight() + 5;
			}

			if (getDmg() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/attack.png");
				assert icon != null;
				int x = 25;

				String val = String.valueOf(getDmg());
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.RED);
				Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				if (this instanceof Senshi s && s.isBlinded()) {
					g2d.setColor(Color.LIGHT_GRAY);
					Graph.drawOutlinedString(g2d, "*", x + icon.getWidth() + 5 + m.stringWidth(val), y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				}

				y -= icon.getHeight() + 5;
			}

			if (getCooldown() > 0) {
				icon = IO.getResourceAsImage("shoukan/icons/cooldown.png");
				assert icon != null;
				int x = 25;

				String val = getCooldown() + " t";
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(new Color(0x00B1EE));
				Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
			}
		}


		{ // RIGHT
			int y = desc ? 225 : 291;
			if (getBlock() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/block.png");
				assert icon != null;
				int x = 200 - icon.getWidth();

				String val = getBlock() + "%";
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.GRAY);
				Graph.drawOutlinedString(g2d, val, x - g2d.getFontMetrics().stringWidth(val) - 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				y -= icon.getHeight() + 5;
			}

			if (getDodge() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/dodge.png");
				assert icon != null;
				int x = 200 - icon.getWidth();

				String val = getDodge() + "%";
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.ORANGE);
				Graph.drawOutlinedString(g2d, val, x - g2d.getFontMetrics().stringWidth(val) - 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				y -= icon.getHeight() + 5;
			}

			if (getCurses().size() > 0) {
				icon = IO.getResourceAsImage("shoukan/icons/curse.png");
				assert icon != null;
				int x = 200 - icon.getWidth();

				String val = String.valueOf(getCurses().size());
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.BLACK);
				Graph.drawOutlinedString(g2d, val, x - g2d.getFontMetrics().stringWidth(val) - 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.WHITE);
			}
		}
	}

	default String getString(I18N locale, String key, Object... params) {
		LocalizedString str = DAO.find(LocalizedString.class, new LocalizedId(key.toLowerCase(Locale.ROOT), locale));
		if (str != null) {
			return str.getValue().formatted(params);
		} else {
			return "";
		}
	}

	default String processTags(I18N locale) {
		List<String> tags = getTags();
		List<String> out = new ArrayList<>();

		for (String tag : tags) {
			if (tag.startsWith("race/")) {
				out.add(locale.get(tag).toUpperCase(Locale.ROOT));
			} else if (tag.startsWith("tag/")) {
				out.add(getString(locale, tag).toUpperCase(Locale.ROOT));
			}

			if (out.toString().length() > 32) {
				out.remove(out.size() - 1);
				out.add("...");
				break;
			}
		}

		return out.toString();
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

	default Source asSource(Trigger trigger) {
		return new Source(this, trigger);
	}

	default Target asTarget(Trigger trigger) {
		if (this instanceof Senshi s) {
			return new Target(s, trigger);
		} else {
			return new Target();
		}
	}
}
