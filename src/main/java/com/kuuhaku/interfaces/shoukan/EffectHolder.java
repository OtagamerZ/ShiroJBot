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
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONArray;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public interface EffectHolder<T extends Drawable<T>> extends Drawable<T> {
	Map<String, Color> COLORS = new HashMap<>() {{
		put("php", new Color(0x85C720));
		put("pmp", new Color(0x3F9EFF));
		put("pdg", new Color(0x9A1313));
		put("prg", new Color(0x7ABCFF));

		put("hp", new Color(0xFF0000));
		put("mp", new Color(0x3F9EFE));
		put("atk", new Color(0xFE0000));
		put("dfs", new Color(0x00C500));
		put("ddg", new Color(0xFFC800));
		put("blk", new Color(0xA9A9A9));

		put("b", Color.BLACK);
		put("n", Color.BLACK);
		put("cd", new Color(0x48BAFF));
		put("ally", new Color(0x000100));
		put("enemy", new Color(0x010000));
	}};

	boolean execute(EffectParameters ep);

	default void executeAssert(Trigger trigger) {
	}

	default Hand getLeech() {
		return null;
	}

	default void setLeech(Hand leech) {
	}

	default Function<String, String> parseValues(Graphics2D g2d, Deck deck, Drawable<?> d) {
		return str -> {
			JSONObject groups = Utils.extractNamedGroups(str, "(?:\\{=(?<calc>(?:(?!}).)+)})?(?:\\{(?<color>\\w+)})?");

			g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 10));
			g2d.setColor(deck.getStyling().getFrame().getSecondaryColor());
			if (!groups.isEmpty()) {
				String val;
				try {
					@Language("Groovy") String calc = groups.getString("calc");
					if (!calc.isBlank()) {
						Hand h = d.getHand();

						calc = "import static java.lang.Math.*\n\n" + calc;
						val = String.valueOf(
								Utils.eval(calc, new HashMap<>() {{
									put("bhp", h == null ? 5000 : h.getBase().hp());
									put("pmp", h == null ? 5 : h.getMP());
									put("php", h == null ? 5000 : h.getHP());
									put("pdg", h == null ? 0 : Math.max(0, -h.getRegDeg().peek()));
									put("prg", h == null ? 0 : Math.max(0, h.getRegDeg().peek()));
									put("mp", d.getMPCost());
									put("hp", d.getHPCost());
									put("atk", d.getDmg());
									put("dfs", d.getDfs());
									put("ddg", d.getDodge());
									put("blk", d.getBlock());
								}})
						);

						double pow = d instanceof Senshi s ? s.getPower() : 1;
						val = StringUtils.abbreviate(
								str.replaceFirst("\\{.+}", String.valueOf(Math.round(NumberUtils.toDouble(val) * pow))),
								Drawable.MAX_DESC_LENGTH
						);
					} else {
						val = str;
					}

					g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 10));
					String color = groups.getString("color", "");
					g2d.setColor(COLORS.getOrDefault(color, g2d.getColor()));

					if (!Utils.equalsAny(color, "", "b", "n")) {
						val = val + "    ";
					}

					if (color.equalsIgnoreCase("n")) {
						val += Constants.VOID;
					} else if (!Utils.equalsAny(color, "ally", "enemy")) {
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
			str = str.replace("_", " ");
			if (lastVal.get() != y) {
				line.getAndIncrement();
				lastVal.set(y);
			}

			if (!legacy && line.get() == 6) {
				x += 10;
			}

			if (str.startsWith(Constants.VOID)) {
				if (Calc.luminance(g2d.getColor()) < 0.2) {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(255, 255, 255));
				} else {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(0, 0, 0));
				}
			} else if (str.endsWith(Constants.VOID)) {
				Graph.drawOutlinedString(g2d, str, x, y, 1.1f, g2d.getColor());
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
				g2d.drawImage(icon, x + g2d.getFontMetrics().stringWidth(str.replace(" ", "")) + 1, y - size + 1, size, size, null);
			}
		};
	}

	default JSONObject extractValues(I18N locale, Drawable<?> d) {
		JSONObject out = new JSONObject();

		String desc = d.getDescription(locale);
		for (String str : desc.split("\\s")) {
			JSONObject groups = Utils.extractNamedGroups(str, "(?:\\{=(?<calc>(?:(?!}).)+)})?(?:\\{(?<key>\\w+)})?");

			if (!groups.isEmpty()) {
				try {
					@Language("Groovy") String calc = groups.getString("calc");
					if (!calc.isBlank()) {
						Hand h = d.getHand();

						calc = "import static java.lang.Math.*\n\n" + calc;
						String val = String.valueOf(
								Utils.eval(calc, new HashMap<>() {{
									put("bhp", h == null ? 5000 : h.getBase().hp());
									put("pmp", h == null ? 5 : h.getMP());
									put("php", h == null ? 5000 : h.getHP());
									put("pdg", h == null ? 0 : Math.max(0, -h.getRegDeg().peek()));
									put("prg", h == null ? 0 : Math.max(0, h.getRegDeg().peek()));
									put("mp", d.getMPCost());
									put("hp", d.getHPCost());
									put("atk", d.getDmg());
									put("dfs", d.getDfs());
									put("ddg", d.getDodge());
									put("blk", d.getBlock());
								}})
						);

						double pow = d instanceof Senshi s ? s.getPower() : 1;
						out.compute(groups.getString("key"), (k, v) -> {
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
