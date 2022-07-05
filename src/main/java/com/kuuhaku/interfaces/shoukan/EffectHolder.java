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
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONArray;
import org.apache.logging.log4j.util.TriConsumer;

import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public interface EffectHolder {
	boolean execute(EffectParameters ep);

	default Function<String, String> parseValues(Graphics2D g2d, Deck deck, Drawable<?> d) {
		return s -> {
			JSONArray groups = Utils.extractGroups(s, "\\{((?:(?!}).)+)}(?:\\{(\\w+)})?");

			g2d.setColor(deck.getFrame().getSecondaryColor());
			if (!groups.isEmpty()) {
				String val = String.valueOf(
						Utils.eval(groups.getString(0), Map.of(
								"mp", d.getMPCost(),
								"hp", d.getHPCost(),
								"atk", d.getDmg(),
								"def", d.getDef(),
								"ddg", d.getDodge(),
								"blk", d.getBlock(),
								"pow", d.getPower()
						))
				);

				if (groups.size() > 1) {
					switch (groups.getString(1)) {
						case "mp" -> g2d.setColor(new Color(0x00C5C5));
						case "hp" -> g2d.setColor(new Color(0x199452));
						case "atk" -> g2d.setColor(new Color(0xD70000));
						case "def" -> g2d.setColor(new Color(0x00C500));
						case "ddg" -> g2d.setColor(new Color(0xFFC800));
						case "blk" -> g2d.setColor(new Color(0x777777));
						case "pow" -> g2d.setColor(new Color(0x8624EE));
					}
				}

				return Constants.VOID + s.replaceFirst("\\{.+}", Utils.roundToString(Double.parseDouble(val), 2))
						.replace("_", " ");
			}

			return s;
		};
	}

	default TriConsumer<String, Integer, Integer> highlightValues(Graphics2D g2d) {
		return (str, x, y) -> {
			if (str.startsWith(Constants.VOID) && !g2d.getColor().equals(Color.BLACK)) {
				Graph.drawOutlinedString(g2d, str, x, y, 1, new Color(0, 0, 0, 200));
			} else {
				g2d.drawString(str, x, y);
			}
		};
	}
}
