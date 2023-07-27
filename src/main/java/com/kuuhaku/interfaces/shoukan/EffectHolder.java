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

import com.kuuhaku.model.common.CachedScriptManager;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Charm;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.DeckStyling;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.TriConsumer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.kuuhaku.model.enums.shoukan.Trigger.ON_EMPOWER;

public interface EffectHolder<T extends Drawable<T>> extends Drawable<T> {
	Pair<Integer, Color> EMPTY = new Pair<>(-1, Color.BLACK);

	Map<String, Pair<Integer, Color>> COLORS = Map.ofEntries(
			Map.entry("php", new Pair<>(0, new Color(0x85C720))),
			Map.entry("bhp", new Pair<>(1, new Color(0x85C720))),
			Map.entry("pmp", new Pair<>(2, new Color(0x3F9EFF))),
			Map.entry("pdg", new Pair<>(3, new Color(0x9A1313))),
			Map.entry("prg", new Pair<>(4, new Color(0x7ABCFF))),

			Map.entry("hp", new Pair<>(5, new Color(0xFF0000))),
			Map.entry("mp", new Pair<>(6, new Color(0x3F9EFE))),
			Map.entry("atk", new Pair<>(7, new Color(0xFF0000))),
			Map.entry("dfs", new Pair<>(8, new Color(0x00C500))),
			Map.entry("ddg", new Pair<>(9, new Color(0xFFC800))),
			Map.entry("blk", new Pair<>(10, new Color(0xA9A9A9))),
			Map.entry("cd", new Pair<>(11, new Color(0x48BAFF))),

			Map.entry("ally", new Pair<>(12, Color.BLACK)),
			Map.entry("enemy", new Pair<>(13, Color.BLACK)),
			Map.entry("b", EMPTY),
			Map.entry("n", EMPTY)
	);

	String DC1 = "\u200B";
	String DC2 = "\u200C";
	String[] ICONS = {
			"hp.png", "hp.png", "mp.png",
			"degen.png", "regen.png", "blood.png",
			"mana.png", "attack.png", "defense.png",
			"dodge.png", "block.png", "cooldown.png",
			"ally_target.png", "enemy_target.png"
	};

	CardAttributes getBase();

	CardExtra getStats();

	default void setFlag(Flag flag, boolean value) {
		setFlag(flag, value, false);
	}

	default void setFlag(Flag flag, boolean value, boolean permanent) {
		if (flag == Flag.EMPOWERED && value) {
			getGame().trigger(ON_EMPOWER, asSource(ON_EMPOWER));
		}

		getStats().setFlag(flag, value, permanent);
	}

	default boolean popFlag(Flag flag) {
		return getStats().popFlag(flag);
	}

	Hand getLeech();

	void setLeech(Hand leech);

	default boolean hasFlag(Flag flag) {
		return getStats().hasFlag(flag);
	}

	boolean hasCharm(Charm charm);

	default boolean isPassive() {
		return getBase().getTags().contains("PASSIVE");
	}

	default boolean isFixed() {
		return getBase().getTags().contains("FIXED");
	}

	boolean execute(EffectParameters ep);

	default void executeAssert(Trigger trigger) {
	}

	CachedScriptManager getCSM();

	default String parseDescription(I18N locale) {
		Hand h = getHand();
		boolean inGame = h.getGame() != null;

		Map<String, Object> values = Map.ofEntries(
				Map.entry("php", inGame ? h.getHP() : 6000),
				Map.entry("bhp", inGame ? h.getBase().hp() : 6000),
				Map.entry("pmp", inGame ? h.getMP() : 5),
				Map.entry("pdg", inGame ? Math.max(0, -h.getRegDeg().peek()) : 0),
				Map.entry("prg", inGame ? Math.max(0, h.getRegDeg().peek()) : 0),
				Map.entry("mp", getMPCost()),
				Map.entry("hp", getHPCost()),
				Map.entry("atk", getDmg()),
				Map.entry("dfs", getDfs()),
				Map.entry("ddg", getDodge()),
				Map.entry("blk", getBlock()),
				Map.entry("pow", getPower()),
				Map.entry("data", getStats().getData())
		);

		CachedScriptManager csm = getCSM();
		if (csm.getPropHash().intValue() != values.hashCode()) {
			csm.getStoredProps().clear();
		}

		String desc = getDescription(locale);
		Matcher pat = Utils.regex(desc, "(?:\\{=(.*?)}|\\{(\\w+)})(%)?");

		return pat.replaceAll(m -> {
			boolean tag = m.group(2) != null;
			boolean prcnt = m.group(3) != null;
			String str = tag ? m.group(2) : m.group(1);

			String out = "";
			if (!tag) {
				LinkedHashSet<Object> types = new LinkedHashSet<>(Utils.extractGroups(str, "\\$(\\w+)"));
				String main = types.stream().map(String::valueOf).findFirst().orElse(null);

				JSONObject props = csm.getStoredProps();
				if (!Utils.equalsAny(main, props.keySet())) {
					String val = String.valueOf(Utils.exec("import static java.lang.Math.*\n\n" + str.replace("$", ""), values));

					for (Object type : types) {
						props.compute(String.valueOf(type), (k, v) -> {
							int value = Calc.round(NumberUtils.toDouble(val) * getPower());

							if (v == null) {
								return value;
							} else if (v instanceof JSONArray a) {
								a.add(value);
								return a;
							}

							return new JSONArray(List.of(v, value));
						});
					}
				}

				Number val;
				Object prop = props.get(main, "");
				if (prop instanceof JSONArray a) {
					val = NumberUtils.toDouble(String.valueOf(a.remove(0)));
				} else {
					val = NumberUtils.toDouble(String.valueOf(prop));
				}

				out = String.valueOf(Calc.round(val.doubleValue() * getPower()));
				if (prcnt) {
					out += "%";
				}

				out += types.stream()
						.filter(COLORS::containsKey)
						.map(t -> "§" + Character.toString(0x2801 + COLORS.get(t).getFirst()))
						.collect(Collectors.joining());
			} else {
				Pair<Integer, Color> idx = COLORS.get(str);

				if (idx != null) {
					if (idx.getFirst() != -1) {
						out = "§" + Character.toString(0x2801 + idx.getFirst());
					} else {
						out = switch (str) {
							case "b" -> DC1;
							case "n" -> DC2;
							default -> "";
						};
					}
				}
			}

			return Matcher.quoteReplacement(out);
		});
	}

	default TriConsumer<String, Integer, Integer> highlightValues(Graphics2D g2d, boolean legacy) {
		DeckStyling style = getHand() == null ? new DeckStyling() : getHand().getUserDeck().getStyling();
		AtomicInteger lastVal = new AtomicInteger();
		AtomicInteger line = new AtomicInteger();

		return (str, x, y) -> {
			if (lastVal.get() != y) {
				line.getAndIncrement();
				lastVal.set(y);
			}

			if (!legacy && line.get() == (getTags().isEmpty() ? 7 : 6)) {
				x += 10;
			}

			g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 10));
			g2d.setColor(style.getFrame().getSecondaryColor());

			FontMetrics fm = g2d.getFontMetrics();
			try {
				if (str.contains(DC1)) {
					g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.PLAIN, 10));

					if (Calc.luminance(g2d.getColor()) < 0.2) {
						Graph.drawOutlinedString(g2d, str, x, y, 1.5f, Color.WHITE);
					} else {
						Graph.drawOutlinedString(g2d, str, x, y, 1.5f, Color.BLACK);
					}

					return;
				} else if (str.contains(DC2)) {
					Graph.drawOutlinedString(g2d, str, x, y, 0.125f, g2d.getColor());
					return;
				}
			} finally {
				g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 10));
			}

			if (str.contains("§")) {
				Collection<Pair<Integer, Color>> cols = COLORS.values();
				List<Color> types = Utils.extractGroups(str, "([⠁-⣿])").parallelStream()
						.map(o -> String.valueOf(o).charAt(0))
						.map(c -> cols.parallelStream()
								.filter(p -> p.getFirst() == c - 0x2801)
								.map(Pair::getSecond)
								.findAny().orElse(null)
						)
						.filter(Objects::nonNull)
						.toList();

				for (String s : str.split("§(?=[⠁-⣿])|(?<=[⠁-⣿])")) {
					if (s.length() == 0) continue;

					char code = s.charAt(0);
					if (Utils.between(code, 0x2801, 0x2900)) {
						String path = "shoukan/icons/" + ICONS[s.charAt(0) - 0x2801];

						BufferedImage icon = IO.getResourceAsImage(path);
						if (icon != null) {
							int size = g2d.getFont().getSize();
							g2d.drawImage(icon, x + 2, y - size + 1, size, size, null);
							x += size + 2;
						}
					} else {
						g2d.setColor(Graph.mix(types));
						if (!g2d.getColor().equals(Color.BLACK)) {
							Graph.drawOutlinedString(g2d, s, x, y, 1.5f, Color.BLACK);
						}

						g2d.drawString(s, x, y);
						x += fm.stringWidth(s);
					}
				}
			} else {
				g2d.drawString(str, x, y);
			}
		};
	}

	default void drawDescription(Graphics2D g2d, I18N locale) {
		DeckStyling style = getHand() == null ? new DeckStyling() : getHand().getUserDeck().getStyling();

		g2d.setColor(style.getFrame().getSecondaryColor());
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 11));

		int y = 276;
		String tags = processTags(locale);
		if (tags != null) {
			g2d.drawString(tags, 7, 275);
			y += 11;
		}

		Graph.drawMultilineString(g2d, parseDescription(locale),
				7, y, 211, 3,
				highlightValues(g2d, style.getFrame().isLegacy())
		);
	}
}
