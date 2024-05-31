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

package com.kuuhaku.model.records;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public record DropCondition(String key, Function<RandomGenerator, Object[]> extractor, BiFunction<Object[], Account, Boolean> condition) {
	public String toString(I18N locale, RandomGenerator rng) {
		Object[] vals = extractor().apply(rng);
		String[] strs = new String[vals.length];

		fill: for (int i = 0; i < vals.length; i++) {
			Object val = vals[i];
			for (Method m : val.getClass().getDeclaredMethods()) {
				if (m.getName().equals("toString") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == I18N.class) {
					try {
						strs[i] = String.valueOf(m.invoke(locale));
						continue fill;
					} catch (IllegalAccessException | InvocationTargetException ignored) {
					}
				}
			}

			strs[i] = String.valueOf(vals[i]);
		}

		return locale.get("condition/" + key, strs);
	}
}
