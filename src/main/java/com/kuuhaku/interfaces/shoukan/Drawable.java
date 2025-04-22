/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.common.shoukan.TagBundle;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.*;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.shoukan.CardTag;
import com.kuuhaku.model.records.shoukan.Source;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import org.apache.commons.collections4.set.ListOrderedSet;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface Drawable<T extends Drawable<T>> {
	int MAX_NAME_WIDTH = 175;
	int MAX_DESC_LENGTH = 210;
	Font FONT = Fonts.OPEN_SANS_EXTRABOLD.deriveBold(20);
	int BORDER_WIDTH = 3;
	Rectangle SIZE = new Rectangle(255, 380);

	long getSerial();

	String getId();

	Card getCard();

	default Card getVanity() {
		return getCard();
	}

	default TagBundle getTagBundle() {
		return new TagBundle();
	}

	default Set<String> getTags() {
		return getTagBundle().parallelStream()
				.map(CardTag::name)
				.map(String::toUpperCase)
				.collect(Collectors.toSet());
	}

	default List<String> getTags(I18N locale) {
		return getTagBundle().stream()
				.map(t -> {
					if (!t.prefix().equals("tag")) {
						return locale.get(t.toString());
					}

					return getString(locale, t.toString());
				})
				.filter(s -> !s.isBlank())
				.distinct()
				.toList();
	}

	default SlotColumn getSlot() {
		return new SlotColumn(getGame(), getSide(), -1);
	}

	default void setSlot(SlotColumn slot) {
	}

	Hand getOriginalHand();

	Hand getHand();

	void setHand(Hand hand);

	default Shoukan getGame() {
		if (getHand() == null) return null;

		return getHand().getGame();
	}

	default Side getSide() {
		if (getHand() == null) return null;

		return getHand().getSide();
	}

	default int getIndex() {
		if (getHand() == null) return -1;

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

	default int getParry() {
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

	boolean isAvailable();

	void setAvailable(boolean available);

	default boolean isFlipped() {
		return false;
	}

	default void setFlipped(boolean flipped) {
	}

	default boolean isEthereal() {
		return false;
	}

	default void setEthereal(boolean ethereal) {
	}

	default boolean isManipulated() {
		return false;
	}

	default void setManipulated(boolean manipulated) {
	}

	default void manipulate(Consumer<Drawable<T>> action) {
		try {
			setManipulated(true);
			action.accept(this);
		} finally {
			setManipulated(false);
		}
	}

	StashedCard getStashRef();

	void setStashRef(StashedCard sc);

	BondedList<?> getCurrentStack();

	void setCurrentStack(BondedList<?> stack);

	default boolean checkRemoval(Hand h, boolean allowEth, BondedList<Drawable<?>> stack) {
		return (!allowEth && isEthereal())
			   || !Objects.equals(getHand(), h)
			   || stack != getCurrentStack();
	}

	void reset();

	default BufferedImage render(I18N locale, Deck deck) {
		if (getHand() == null) {
			setHand(new Hand(deck));
		}

		return render(locale);
	}

	BufferedImage render(I18N locale);

	default void drawName(Graphics2D g2d) {
		Deck deck = getOriginalHand().getUserDeck();

		g2d.setFont(FONT);
		g2d.setColor(deck.getFrame().getPrimaryColor());

		String name = Graph.abbreviate(g2d, getVanity().getName(), MAX_NAME_WIDTH);
		Graph.drawOutlinedString(g2d, name, 12, 30, 2, deck.getFrame().getBackgroundColor());

		if (this instanceof EffectHolder<T> eh) {
			if (!eh.getStats().getWrite().isBlank()) {
				g2d.setColor(Color.ORANGE);
				g2d.setFont(Fonts.NOTO_SANS_EXTRABOLD.deriveBold(15f));

				String str = eh.getStats().getWrite();
				FontMetrics fm = g2d.getFontMetrics();
				Graph.drawOutlinedString(g2d, str,
						225 / 2 - fm.stringWidth(str) / 2, 39 + fm.getHeight() / 2,
						2, Color.BLACK
				);
			}
		}
	}

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
		boolean aug = getTagBundle().contains("augment") && getGame() == null;

		{ // LEFT
			int y = desc ? 225 : 291;
			if (getDfs() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/defense.png");
				assert icon != null;
				int x = 25;

				String val = aug ? Utils.sign(getDfs()) : String.valueOf(getDfs());
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.GREEN);
				if (this instanceof Senshi s && s.isDefending()) {
					g2d.setFont(FONT.deriveFont(Map.of(
							TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON
					)));

					Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
					g2d.setFont(FONT);
				} else {
					Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				}

				y -= icon.getHeight() + 5;
			}

			if (getDmg() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/attack.png");
				assert icon != null;
				int x = 25;

				String val = aug ? Utils.sign(getDmg()) : String.valueOf(getDmg());
				g2d.drawImage(icon, x, y, null);
				g2d.setColor(Color.RED);
				if (this instanceof Senshi s && !s.isDefending()) {
					g2d.setFont(FONT.deriveFont(Map.of(
							TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON
					)));

					Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
					g2d.setFont(FONT);
				} else {
					Graph.drawOutlinedString(g2d, val, x + icon.getWidth() + 5, y - 6 + (icon.getHeight() + m.getHeight()) / 2, BORDER_WIDTH, Color.BLACK);
				}

				y -= icon.getHeight() + 5;
			}

			if (this instanceof Senshi s) {
				if (s.isBlinded()) {
					icon = IO.getResourceAsImage("shoukan/icons/blind.png");
					assert icon != null;
					int x = 25;

					g2d.drawImage(icon, x, y, null);
					y -= icon.getHeight() + 5;
				}
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
			if (getParry() != 0) {
				icon = IO.getResourceAsImage("shoukan/icons/parry.png");
				assert icon != null;
				int x = 200 - icon.getWidth();

				String val = (aug ? Utils.sign(getParry()) : getParry()) + "%";
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

			if (!getCurses().isEmpty()) {
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

	default void drawOverlay(Graphics2D g2d) {
		if (getHand() != null && getGame() != null) {
			boolean legacy = getHand().getUserDeck().getFrame().isLegacy();
			String path = "shoukan/frames/state/" + (legacy ? "old" : "new");

			if (this instanceof Senshi s) {
				double mult = s.getFieldMult();
				if (mult != 1) {
					BufferedImage indicator = null;
					if (mult > 1) {
						indicator = IO.getResourceAsImage(path + "/buffed.png");
					} else if (mult < 1) {
						indicator = IO.getResourceAsImage(path + "/nerfed.png");
					}

					g2d.drawImage(indicator, 0, 0, null);
				}
			}

			if (this instanceof EffectHolder<?> eh && eh.hasFlag(Flag.EMPOWERED)) {
				BufferedImage ovr = IO.getResourceAsImage(path + "/empowered.png");
				g2d.drawImage(ovr, 0, 0, null);
			}

			if (isEthereal()) {
				BufferedImage ovr = IO.getResourceAsImage(path + "/ethereal.png");
				g2d.drawImage(ovr, 0, 0, null);
			}

			if (isManipulated()) {
				BufferedImage ovr = IO.getResourceAsImage("shoukan/states/locked.png");
				g2d.drawImage(ovr, 15, 15, null);
			}
		}
	}

	default String getString(I18N locale, String key, Object... params) {
		if (key == null) return "";
		return LocalizedString.get(locale, key, "").formatted(params);
	}

	default String processTags(I18N locale) {
		List<String> tags = getTags(locale);
		if (tags.isEmpty()) return null;

		List<String> out = new ArrayList<>();
		for (String tag : tags) {
			out.add(tag.toUpperCase());

			if (out.toString().length() > 32) {
				out.removeLast();
				out.add("...");
				break;
			}
		}

		return out.toString();
	}

	T fork() throws CloneNotSupportedException;

	@SuppressWarnings("unchecked")
	default T copy() {
		try {
			return fork();
		} catch (Exception e) {
			return (T) this;
		}
	}

	default T withCopy(Consumer<T> act) {
		T t = copy();
		act.accept(t);
		return t;
	}

	default Source asSource() {
		return asSource(Trigger.NONE);
	}

	default Source asSource(Trigger trigger) {
		return new Source(this, trigger);
	}

	default Target asTarget() {
		return asTarget(Trigger.NONE);
	}

	default Target asTarget(Trigger trigger) {
		return asTarget(trigger, getSide() == getGame().getCurrentSide() ? TargetType.ALLY : TargetType.ENEMY);
	}

	default Target asTarget(Trigger trigger, TargetType type) {
		if (this instanceof Senshi s) {
			return new Target(s, s.getIndex(), trigger, type);
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
		if (getGame() == null || !(this instanceof EffectHolder<?> eh) || getIndex() == -1) return List.of();
		xray |= side == getSide();

		boolean empower = eh.hasFlag(Flag.EMPOWERED);
		Set<Senshi> tgts = new HashSet<>();
		for (int idx : indexes) {
			if (idx < 0 || idx > 4) continue;

			SlotColumn slt = getGame().getSlots(side).get(idx);

			Senshi tgt;
			if (top == null) {
				if (xray) {
					for (Senshi s : slt.getCards()) {
						addTarget(s, tgts, empower);
					}

					continue;
				} else {
					tgt = slt.getUnblocked();
				}
			} else if (!top) {
				if (slt.hasTop() && !xray) continue;

				tgt = slt.getBottom();
			} else {
				tgt = slt.getTop();
			}

			addTarget(tgt, tgts, empower);
		}

		return tgts.stream().sorted(Comparator.comparingInt(Senshi::getIndex)).toList();
	}

	default List<Evogear> getEquipments(Side side) {
		return getEquipments(side, 0, 1, 2, 3, 4);
	}

	default List<Evogear> getEquipments(Side side, int... indexes) {
		if (!(this instanceof EffectHolder<?> eh) || getIndex() == -1) return null;

		boolean empower = eh.hasFlag(Flag.EMPOWERED);
		List<Evogear> tgts = new ArrayList<>();
		for (int idx : indexes) {
			if (idx < 0 || idx > 4) continue;

			SlotColumn slt = getGame().getSlots(side).get(idx);
			Senshi tgt = slt.getTop();

			if (tgt != null) {
				tgts.addAll(tgt.getEquipments());
			}
		}

		return List.copyOf(tgts);
	}

	private void addTarget(Senshi tgt, Set<Senshi> targets, boolean empower) {
		if (tgt == null || !(this instanceof EffectHolder<T> eh) || tgt.isProtected(eh)) return;

		targets.add(tgt);

		if (empower) {
			for (Senshi s : tgt.getNearby()) {
				if (!s.isProtected(eh)) {
					targets.add(s);
				}
			}
		}
	}
}
