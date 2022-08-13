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
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.function.Function;

public interface EffectHolder<T extends Drawable<T>> extends Drawable<T> {
	boolean execute(EffectParameters ep);

	default boolean executeAssert(Trigger trigger) {
		return true;
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
			g2d.setColor(deck.getFrame().getSecondaryColor());
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
									put("dfs", d.getDef());
									put("ddg", d.getDodge());
									put("blk", d.getBlock());
								}})
						);

						val = StringUtils.abbreviate(
								str.replaceFirst("\\{.+}", String.valueOf(Math.round(NumberUtils.toDouble(val)))),
								Drawable.MAX_DESC_LENGTH
						);
					} else {
						val = str;
					}

					g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 10));
					String color = groups.getString("color", "");
					switch (color) {
						case "php" -> g2d.setColor(new Color(0x85C720));
						case "pmp" -> g2d.setColor(new Color(0x3F9EFF));
						case "pdg" -> g2d.setColor(new Color(0x9A1313));
						case "prg" -> g2d.setColor(new Color(0x7ABCFF));

						case "hp" -> g2d.setColor(new Color(0xFF0000));
						case "mp" -> g2d.setColor(new Color(0x3F9EFE));
						case "atk" -> g2d.setColor(new Color(0xFE0000));
						case "dfs" -> g2d.setColor(new Color(0x00C500));
						case "ddg" -> g2d.setColor(new Color(0xFFC800));
						case "blk" -> g2d.setColor(new Color(0xA9A9A9));

						case "b" -> g2d.setColor(new Color(0x010101));
						case "cd" -> g2d.setColor(new Color(0x48BAFF));

						case "ally" -> g2d.setColor(new Color(0x000100));
						case "enemy" -> g2d.setColor(new Color(0x000001));
					}

					if (!Utils.equalsAny(color, "", "b")) {
						val = val + "    ";
					}

					if (!Utils.equalsAny(color, "ally", "enemy")) {
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

	default TriConsumer<String, Integer, Integer> highlightValues(Graphics2D g2d) {
		return (str, x, y) -> {
			if (str.startsWith(Constants.VOID)) {
				if (Calc.luminance(g2d.getColor()) < 0.2) {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(255, 255, 255));
				} else {
					Graph.drawOutlinedString(g2d, str, x, y, 1.5f, new Color(0, 0, 0));
				}
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
				case 0x000001 -> IO.getResourceAsImage("shoukan/icons/enemy_target.png");
				default -> null;
			};

			if (icon != null) {
				int size = g2d.getFont().getSize();
				g2d.drawImage(icon, x + g2d.getFontMetrics().stringWidth(str.replace(" ", "")) + 1, y - size + 1, size, size, null);
			}
		};
	}
}
