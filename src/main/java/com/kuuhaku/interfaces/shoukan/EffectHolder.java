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

import com.kuuhaku.Constants;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.DeckStyling;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONArray;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public interface EffectHolder<T extends Drawable<T>> extends Drawable<T> {
	Map<String, Color> COLORS = Map.ofEntries(
		Map.entry("php", new Color(0x85C720)),
		Map.entry("pmp", new Color(0x3F9EFF)),
		Map.entry("pdg", new Color(0x9A1313)),
		Map.entry("prg", new Color(0x7ABCFF)),

		Map.entry("hp", new Color(0xFF0000)),
		Map.entry("mp", new Color(0x3F9EFE)),
		Map.entry("atk", new Color(0xFE0000)),
		Map.entry("dfs", new Color(0x00C500)),
		Map.entry("ddg", new Color(0xFFC800)),
		Map.entry("blk", new Color(0xA9A9A9)),

		Map.entry("b", Color.BLACK),
		Map.entry("n", Color.BLACK),
		Map.entry("cd", new Color(0x48BAFF)),
		Map.entry("ally", new Color(0x000100)),
		Map.entry("enemy", new Color(0x010000))
	);

	CardAttributes getBase();

	CardExtra getStats();

	Hand getLeech();

	void setLeech(Hand leech);

	boolean execute(EffectParameters ep);

	default void executeAssert(Trigger trigger) {
	}

	default Function<String, String> parseValues(Graphics2D g2d, DeckStyling style, JSONObject values) {
		return str -> {
			JSONObject groups = Utils.extractNamedGroups(str, "\\{=(?<calc>.*?\\$(?<type>\\w+).*?)}|\\{(?<tag>\\w+)}");

			g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 10));
			g2d.setColor(style.getFrame().getSecondaryColor());
			if (!groups.isEmpty()) {
				String type = groups.getString("type");
				String tag = groups.getString("tag");

				String val;
				try {
					Object obj = values.get(type);
					if (!type.isBlank() && obj != null) {
						String v;
						if (obj instanceof JSONArray a) {
							v = String.valueOf(a.remove(0));
						} else {
							v = String.valueOf(obj);
						}

						val = str.replaceFirst("\\{.+}", Utils.roundToString(NumberUtils.toDouble(v), 0));
					} else {
						val = str;
					}

					g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 10));
					g2d.setColor(COLORS.getOrDefault(Utils.getOr(tag, type), g2d.getColor()));

					if (!Utils.equalsAny(tag, "b", "n")) {
						val = val + "    ";
					}

					if (tag.equalsIgnoreCase("n")) {
						val += Constants.VOID;
					} else if (!Utils.equalsAny(tag, "ally", "enemy")) {
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

			if (!legacy && line.get() == 6) {
				x += 10;
			}

			if (str.startsWith(Constants.VOID)) {
				if (Calc.luminance(g2d.getColor()) < 0.2) {
					Graph.drawOutlinedString(g2d, str, x, y - 1, 1.5f, new Color(255, 255, 255));
				} else {
					Graph.drawOutlinedString(g2d, str, x, y - 1, 1.5f, new Color(0, 0, 0));
				}
			} else if (str.endsWith(Constants.VOID)) {
				Graph.drawOutlinedString(g2d, str, x, y - 1, 0.25f, g2d.getColor());
			} else {
				g2d.drawString(str, x, y);
			}

			BufferedImage icon = switch (g2d.getColor().getRGB() & 0xFFFFFF) {
				case 0x85C720 -> IO.getResourceAsImage("shoukan/icons/hp.png");
				case 0x3F9EFF -> IO.getResourceAsImage("shoukan/icons/mp.png");
				case 0x9A1313 -> IO.getResourceAsImage("shoukan/icons/regen.png");
				case 0x7ABCFF -> IO.getResourceAsImage("shoukan/icons/degen.png");
				case 0xFF0000 -> IO.getResourceAsImage("shoukan/icons/blood.png");
				case 0x3F9EFE -> IO.getResourceAsImage("shoukan/icons/mana.png");
				case 0xFE0000 -> IO.getResourceAsImage("shoukan/icons/attack.png");
				case 0x00C500 -> IO.getResourceAsImage("shoukan/icons/defense.png");
				case 0xFFC800 -> IO.getResourceAsImage("shoukan/icons/dodge.png");
				case 0xA9A9A9 -> IO.getResourceAsImage("shoukan/icons/block.png");
				case 0x48BAFF -> IO.getResourceAsImage("shoukan/icons/cooldown.png");
				case 0x000100 -> IO.getResourceAsImage("shoukan/icons/ally_target.png");
				case 0x010000 -> IO.getResourceAsImage("shoukan/icons/enemy_target.png");
				default -> null;
			};

			if (icon != null) {
				int size = g2d.getFont().getSize();
				g2d.drawImage(icon, x + g2d.getFontMetrics().stringWidth(str) + 1, y - size, size, size, null);
			}
		};
	}

	default JSONObject extractValues(I18N locale, Drawable<?> d) {
		JSONObject out = new JSONObject();

		String desc = d.getDescription(locale);
		for (String str : desc.split("\\s")) {
			JSONObject groups = Utils.extractNamedGroups(str, "\\{=(?<calc>.*?\\$(?<type>\\w+).*?)}|\\{(?<tag>\\w+)}");

			if (!groups.isEmpty()) {
				try {
					@Language("Groovy") String calc = groups.getString("calc").replace("$", "");
					if (!calc.isBlank()) {
						Hand h = d.getHand();

						calc = "import static java.lang.Math.*\n\n" + calc;
						String val = String.valueOf(
								Utils.exec(calc, Map.ofEntries(
										Map.entry("bhp", h == null ? 5000 : h.getBase().hp()),
										Map.entry("pmp", h == null ? 5 : h.getMP()),
										Map.entry("php", h == null ? 5000 : h.getHP()),
										Map.entry("pdg", h == null ? 0 : Math.max(0, -h.getRegDeg().peek())),
										Map.entry("prg", h == null ? 0 : Math.max(0, h.getRegDeg().peek())),
										Map.entry("mp", d.getMPCost()),
										Map.entry("hp", d.getHPCost()),
										Map.entry("atk", d.getDmg()),
										Map.entry("dfs", d.getDfs()),
										Map.entry("ddg", d.getDodge()),
										Map.entry("blk", d.getBlock())
								))
						);

						double pow = d instanceof Senshi s ? s.getPower() : 1;
						out.compute(groups.getString("type"), (k, v) -> {
							int value = Calc.round(NumberUtils.toDouble(val) * pow);

							if (v == null) {
								return value;
							} else if (v instanceof JSONArray a) {
								a.add(value);
								return a;
							}

							return new JSONArray(List.of(v, value));
						});
					}
				} catch (Exception ignore) {
				}
			}
		}

		return out;
	}
}
