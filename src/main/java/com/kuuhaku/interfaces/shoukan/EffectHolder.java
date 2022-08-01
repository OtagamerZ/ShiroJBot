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
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public interface EffectHolder<T extends Drawable<T>> extends Drawable<T> {
	boolean execute(EffectParameters ep);

	default Hand getLeech() {
		return null;
	}

	default void setLeech(Hand leech) {
	}

	default Function<String, String> parseValues(Graphics2D g2d, Deck deck, Drawable<?> d) {
		return str -> {
			JSONObject groups = Utils.extractNamedGroups(str, "(?:\\{=(?<calc>(?:(?!}).)+)})?(?:\\{(?<color>\\w+)})?");

			g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 10));
			g2d.setColor(deck.getFrame().getSecondaryColor());
			if (!groups.isEmpty()) {
				str = Constants.VOID + str;

				String val;
				try {
					@Language("Groovy") String calc = groups.getString("calc");
					if (!calc.isBlank()) {
						if (calc.startsWith(">")) {
							calc = "Math.max(0," + calc.substring(1) + ")";
						} else if (calc.startsWith("<")) {
							calc = "Math.min(" + calc.substring(1) + ",0)";
						}

						val = String.valueOf(
								Utils.eval(calc, Map.of(
										"mp", d.getMPCost(),
										"hp", d.getHPCost(),
										"atk", d.getDmg(),
										"dfs", d.getDef(),
										"ddg", d.getDodge(),
										"blk", d.getBlock()
								))
						);

						val = StringUtils.abbreviate(
								str.replaceFirst("\\{.+}", Utils.roundToString(Double.parseDouble(val), 2)),
								Drawable.MAX_DESC_LENGTH
						);
					} else {
						val = str;
					}

					g2d.setFont(Fonts.OPEN_SANS_EXTRABOLD.deriveFont(Font.BOLD, 10));
					switch (groups.getString("color", "")) {
						case "mp" -> g2d.setColor(new Color(0x3F9EFF));
						case "hp" -> g2d.setColor(new Color(0x85C720));
						case "atk" -> g2d.setColor(new Color(0xFF0000));
						case "dfs" -> g2d.setColor(new Color(0x00C500));
						case "ddg" -> g2d.setColor(new Color(0xFFC800));
						case "blk" -> g2d.setColor(new Color(0xA9A9A9));
						case "b" -> g2d.setColor(new Color(0x010101));
					}

					return val.replaceAll("\\{.+}", "");
				} catch (Exception e) {
					return StringUtils.abbreviate(str, Drawable.MAX_DESC_LENGTH);
				}
			}

			return str;
		};
	}

	default TriConsumer<String, Integer, Integer> highlightValues(Graphics2D g2d) {
		return (str, x, y) -> {
			if (str.startsWith(Constants.VOID) && !g2d.getColor().equals(Color.BLACK)) {
				if (Calc.luminance(g2d.getColor()) < 0.2) {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(255, 255, 255));
				} else {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(0, 0, 0));
				}
			} else {
				g2d.drawString(str, x, y);
			}
		};
	}
}
