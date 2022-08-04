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

package com.kuuhaku.util;

import com.kuuhaku.Constants;

import java.lang.reflect.Method;
import java.util.Collection;

@SuppressWarnings("rawtypes")
public class Copier<C extends Collection, T extends Cloneable> {
	private final Class<C> klass;
	private final Class<T> subklass;

	public Copier(Class<C> klass, Class<T> subklass) {
		this.klass = klass;
		this.subklass = subklass;
	}

	@SuppressWarnings("unchecked")
	public <O extends Collection<T>> O makeCopy(C col) {
		try {
			Method clone = subklass.getDeclaredMethod("clone");

			O out = (O) klass.getDeclaredConstructor().newInstance();
			for (Object t : col) {
				out.add(subklass.cast(clone.invoke(t)));
			}

			return out;
		} catch (Exception e) {
			Constants.LOGGER.error(e, e);
			return null;
		}
	}
}
