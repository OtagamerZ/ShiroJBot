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

import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import org.apache.logging.log4j.util.TriConsumer;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public interface EffectHolder {
	boolean execute(EffectParameters ep);

	default Function<String, String> parseValues(CardExtra stats) {
		return s -> {
			@Language("Groovy") String str = Utils.extract(s, "\\{(.+)}", 1);

			if (str != null) {
				Object val = Utils.eval(str, Map.of(
						"mp", stats.getMana(),
						"hp", stats.getBlood(),
						"atk", stats.getAtk(),
						"def", stats.getDef(),
						"ddg", stats.getDodge(),
						"blk", stats.getBlock(),
						"pow", stats.getPower(),
						"tier", stats.getTier()
				));
				System.out.println(val.getClass());

				return "\u200B" + Utils.roundToString(val, 2);
			}

			return s;
		};
	}

	default TriConsumer<String, Integer, Integer> drawValue(Graphics2D g2d) {
		return (str, x, y) -> {
			if (str.startsWith("\u200B")) {
				Graph.drawOutlinedString(g2d, str.substring(1), x, y, 2, Color.BLACK);
			} else {
				g2d.drawString(str, x, y);
			}
		};
	}
}
