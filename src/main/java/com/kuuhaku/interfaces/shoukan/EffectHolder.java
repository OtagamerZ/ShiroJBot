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
import com.kuuhaku.util.Utils;
import org.intellij.lang.annotations.Language;

import java.util.Map;
import java.util.function.Function;

public interface EffectHolder {
	boolean execute(EffectParameters ep);

	default Function<String, String> parseValues(CardExtra stats) {
		return s -> {
			@Language("Groovy") String str = Utils.extract(s, "\\{(.+)}", 1);

			if (str != null) {
				String val = String.valueOf(
						Utils.eval(str, Map.of(
								"mp", stats.getMana(),
								"hp", stats.getBlood(),
								"atk", stats.getAtk(),
								"def", stats.getDef(),
								"ddg", stats.getDodge(),
								"blk", stats.getBlock(),
								"pow", stats.getPower(),
								"tier", stats.getTier()
						))
				);

				return "\u200B" + s.replaceFirst("\\{.+}", Utils.roundToString(Double.parseDouble(val), 2));
			}

			return s;
		};
	}
}
