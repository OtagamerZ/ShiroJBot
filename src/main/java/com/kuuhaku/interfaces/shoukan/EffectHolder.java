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

import com.kuuhaku.Constants;
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
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public interface EffectHolder<T extends Drawable<T>> extends Drawable<T> {
	Pair<Color, String> EMPTY = new Pair<>(null, null);

	Map<String, Pair<Color, String>> COLORS = Map.ofEntries(
			Map.entry("php", new Pair<>(new Color(0x85C720), "shoukan/icons/hp.png")),
			Map.entry("bhp", new Pair<>(new Color(0x85C720), "shoukan/icons/hp.png")),
			Map.entry("pmp", new Pair<>(new Color(0x3F9EFF), "shoukan/icons/mp.png")),
			Map.entry("pdg", new Pair<>(new Color(0x9A1313), "shoukan/icons/degen.png")),
			Map.entry("prg", new Pair<>(new Color(0x7ABCFF), "shoukan/icons/regen.png")),

			Map.entry("hp", new Pair<>(new Color(0xFF0000), "shoukan/icons/blood.png")),
			Map.entry("mp", new Pair<>(new Color(0x3F9EFE), "shoukan/icons/mana.png")),
			Map.entry("atk", new Pair<>(new Color(0xFE0000), "shoukan/icons/attack.png")),
			Map.entry("dfs", new Pair<>(new Color(0x00C500), "shoukan/icons/defense.png")),
			Map.entry("ddg", new Pair<>(new Color(0xFFC800), "shoukan/icons/dodge.png")),
			Map.entry("blk", new Pair<>(new Color(0xA9A9A9), "shoukan/icons/block.png")),
			Map.entry("cd", new Pair<>(new Color(0x48BAFF), "shoukan/icons/cooldown.png")),

			Map.entry("b", EMPTY),
			Map.entry("n", EMPTY),
			Map.entry("ally", EMPTY),
			Map.entry("enemy", EMPTY)
	);

	CardAttributes getBase();

	CardExtra getStats();

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

	CachedScriptManager<T> getCSM();

	default Function<String, String> parseValues(Graphics2D g2d, DeckStyling style, JSONObject values) {
		return str -> {
			JSONObject groups = Utils.extractNamedGroups(str, "\\{=(?<calc>.*?)}|\\{(?<tag>\\w+)}");

			g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 10));
			g2d.setColor(style.getFrame().getSecondaryColor());
			if (!groups.isEmpty()) {
				JSONArray types = new JSONArray();
				if (groups.has("calc")) {
					types.addAll(Utils.extractGroups(groups.getString("calc"), "(\\$\\w+)"));
				} else {
					types.add(groups.getString("tag"));
				}

				String val;
				try {
					Object obj = values.get(types.getString(0));
					if (obj != null) {
						String v;
						if (obj instanceof JSONArray a) {
							v = String.valueOf(a.remove(0));
						} else {
							v = String.valueOf(obj);
						}

						val = str.replaceFirst("\\{.+}", String.valueOf(Math.round(NumberUtils.toFloat(v))));
					} else {
						val = str;
					}

					List<Color> colors = new ArrayList<>();
					for (Object type : types) {
						System.out.println(type);

						if (COLORS.containsKey(type)) {
							colors.add(COLORS.get(type).getFirst());

							if (!Utils.equalsAny(type, "data", "b", "n")) {
								val += " :" + type + ":";
							}
						} else {
							val = "";
						}
					}

					if (!colors.isEmpty()) {
						g2d.setColor(Graph.mix(colors));
						if (!Utils.containsAny(types, "enemy", "ally")) {
							g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.PLAIN, 10));
						}
					}

					if (types.contains("n")) {
						val += Constants.VOID;
					} else if (!Utils.equalsAny(types, "enemy", "ally")) {
						val = Constants.VOID + val;
					}

					return val.replaceAll("\\{.+}", "");
				} catch (Exception e) {
					return str;
				}
			}

			return str;
		};
	}

	default TriConsumer<String, Integer, Integer> highlightValues(Graphics2D g2d, boolean legacy) {
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

			if (str.startsWith(Constants.VOID)) {
				if (Calc.luminance(g2d.getColor()) < 0.2) {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(255, 255, 255));
				} else {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(0, 0, 0));
				}
			} else if (str.endsWith(Constants.VOID)) {
				Graph.drawOutlinedString(g2d, str, x, y, 0.125f, g2d.getColor());
			} else {
				if (str.startsWith(":") && str.endsWith(":")) {
					BufferedImage icon = IO.getResourceAsImage(COLORS.get(str.substring(1, str.length() - 2)).getSecond());
					if (icon != null) {
						int size = g2d.getFont().getSize();
						g2d.drawImage(icon, x, y - size + 1, size, size, null);
					}

					return;
				}

				g2d.drawString(str, x, y);
			}
		};
	}

	default JSONObject extractValues(I18N locale) {
		Hand h = getHand();
		Map<String, Object> values = Map.ofEntries(
				Map.entry("php", h == null ? 6000 : h.getHP()),
				Map.entry("bhp", h == null ? 6000 : h.getBase().hp()),
				Map.entry("pmp", h == null ? 5 : h.getMP()),
				Map.entry("pdg", h == null ? 0 : Math.max(0, -h.getRegDeg().peek())),
				Map.entry("prg", h == null ? 0 : Math.max(0, h.getRegDeg().peek())),
				Map.entry("mp", getMPCost()),
				Map.entry("hp", getHPCost()),
				Map.entry("atk", getDmg()),
				Map.entry("dfs", getDfs()),
				Map.entry("ddg", getDodge()),
				Map.entry("blk", getBlock()),
				Map.entry("data", getStats().getData())
		);

		CachedScriptManager<T> csm = getCSM();
		if (!csm.getStoredProps().isEmpty() && csm.getPropHash().intValue() == values.hashCode()) {
			return csm.getStoredProps();
		}

		csm.getStoredProps().clear();
		String desc = getDescription(locale);
		for (String str : desc.split("\\s")) {
			JSONObject groups = Utils.extractNamedGroups(str, "\\{=(?<calc>.*?)}");

			if (!groups.isEmpty()) {
				@Language("Groovy") String calc = groups.getString("calc").replace("$", "");
				if (!calc.isBlank()) {
					calc = "import static java.lang.Math.*\n\n" + calc;
					String val = String.valueOf(Utils.exec(calc, values));

					for (Object type : Utils.extractGroups(groups.getString("calc"), "(\\$\\w+)")) {
						csm.getStoredProps().compute(String.valueOf(type), (k, v) -> {
							int value;
							if (!k.equals("data")) {
								value = Calc.round(NumberUtils.toDouble(val) * getPower());
							} else {
								value = Calc.round(NumberUtils.toDouble(val));
							}

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
			}
		}

		return csm.getStoredProps();
	}
}
