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
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.shoukan.Source;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import org.apache.commons.collections4.set.ListOrderedSet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface Drawable<T extends Drawable<T>> extends Cloneable {
	int MAX_NAME_WIDTH = 175;
	int MAX_DESC_LENGTH = 210;
	Font FONT = Fonts.OPEN_SANS_EXTRABOLD.deriveFont(Font.BOLD, 20);
	int BORDER_WIDTH = 3;
	Rectangle SIZE = new Rectangle(255, 380);

	long getSerial();

	String getId();

	Card getCard();

	default Card getVanity() {
		return getCard();
	}

	default List<String> getTags() {
		return List.of();
	}

	default SlotColumn getSlot() {
		return new SlotColumn(getHand().getGame(), getSide(), -1);
	}

	default void setSlot(SlotColumn slot) {
	}

	Hand getHand();

	void setHand(Hand hand);

	default Side getSide() {
		return getHand().getSide();
	}

	default int getIndex() {
		return getSlot().getIndex();
	}

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

	default int getDfs() {
		return 0;
	}

	default int getDodge() {
		return 0;
	}

	default int getBlock() {
		return 0;
	}

	default double getCostMult() {
		return 1;
	}

	default double getAttrMult() {
		return 1;
	}

	default double getPower() {
		return 1;
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

	default boolean keepOnDestroy() {
		return true;
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
		boolean aug = getTags().contains("tag/augment") && getHand() == null;

		{ // LEFT
			int y = desc ? 225 : 291;
			if (getDfs() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/defense.png");
				assert icon != null;
				int x = 25;

				String val = aug ? Utils.sign(getDfs()) : String.valueOf(getDfs());
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.GREEN);
				Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				y -= icon.getHeight() + 5;
			}

			if (getDmg() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/attack.png");
				assert icon != null;
				int x = 25;

				String val = aug ? Utils.sign(getDmg()) : String.valueOf(getDmg());
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.RED);
				Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				if (this instanceof Senshi s && s.isBlinded()) {
					g2d.setColor(Color.LIGHT_GRAY);
					Graph.drawOutlinedString(g2d, "*", x + icon.getWidth() + 6 + m.stringWidth(val), y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
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

				String val = (aug ? Utils.sign(getBlock()) : getBlock()) + "%";
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.GRAY);
				Graph.drawOutlinedString(g2d, val, x - g2d.getFontMetrics().stringWidth(val) - 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				y -= icon.getHeight() + 5;
			}

			if (getDodge() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/dodge.png");
				assert icon != null;
				int x = 200 - icon.getWidth();

				String val = (aug ? Utils.sign(getDodge()) : getDodge()) + "%";
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
		if (key == null) return "";
		return LocalizedString.get(locale, key, "").formatted(params);
	}

	default String processTags(I18N locale) {
		List<String> tags = getTags();
		if (tags.isEmpty()) return null;

		List<String> out = new ArrayList<>();

		for (String tag : tags) {
			if (tag.startsWith("race/")) {
				out.add(locale.get(tag).toUpperCase());
			} else if (tag.startsWith("tag/")) {
				out.add(getString(locale, tag).toUpperCase());
			}

			if (out.toString().length() > 32) {
				out.remove(out.size() - 1);
				out.add("...");
				break;
			}
		}

		return out.stream().filter(s -> !s.isBlank()).toList().toString();
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

	default Source asSource() {
		return asSource(null);
	}

	default Source asSource(Trigger trigger) {
		return new Source(this, trigger);
	}

	default Target asTarget() {
		return asTarget(null);
	}

	default Target asTarget(Trigger trigger) {
		return asTarget(trigger, getSide() == getHand().getGame().getCurrentSide() ? TargetType.ALLY : TargetType.ENEMY);
	}

	default Target asTarget(Trigger trigger, TargetType type) {
		if (this instanceof Senshi s) {
			return new Target(s, getSide(), getIndex(), trigger, type);
		} else {
			return new Target();
		}
	}

	static List<String> ids(List<? extends Drawable<?>> cards) {
		return Utils.map(cards, Drawable::getId);
	}

	default List<Senshi> getCards(Side side) {
		return getCards(side, null);
	}

	default List<Senshi> getCards(Side side, Boolean top) {
		return getCards(side, top, false);
	}

	default List<Senshi> getCards(Side side, Boolean top, int... indexes) {
		return getCards(side, top, false, indexes);
	}

	default List<Senshi> getCards(Side side, Boolean top, boolean xray) {
		return getCards(side, top, xray, 0, 1, 2, 3, 4);
	}

	default List<Senshi> getCards(Side side, Boolean top, boolean xray, int... indexes) {
		if (!(this instanceof EffectHolder<?> eh) || getIndex() == -1) return null;

		boolean empower = eh.hasFlag(Flag.EMPOWERED);
		Set<Senshi> tgts = new HashSet<>();
		for (int idx : indexes) {
			if (idx < 0 || idx > 4) continue;

			SlotColumn slt = getHand().getGame().getSlots(side).get(idx);
			if (xray || side == getSide()) {
				for (Senshi tgt : slt.getCards()) {
					if (tgt == null || (side != getSide() && !xray && tgt.isProtected(this))) continue;

					if (tgt.getIndex() == idx) {
						tgts.add(tgt);
					}

					if (empower) {
						for (Senshi s : tgt.getNearby()) {
							if ((side == getSide() && !xray || !s.isProtected(this)) && s.getIndex() == idx) {
								tgts.add(s);
							}
						}
					}
				}
			} else {
				Senshi tgt;
				if (top == null) tgt = slt.getUnblocked();
				else if (top) tgt = slt.getTop();
				else tgt = slt.getBottom();

				if (tgt == null || side != getSide() && tgt.isProtected(this)) continue;

				if (tgt.getIndex() == idx) {
					tgts.add(tgt);
				}

				if (empower) {
					for (Senshi s : tgt.getNearby()) {
						if ((side == getSide() || !s.isProtected(this)) && s.getIndex() == idx) {
							tgts.add(s);
						}
					}
				}
			}
		}

		return List.copyOf(tgts);
	}
}
