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

package com.kuuhaku.model.persistent.converter;

import com.kuuhaku.util.Bit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.EnumSet;

@Converter(autoApply = true)
abstract class FlagConverter<T extends Enum<T>> implements AttributeConverter<EnumSet<T>, Integer> {
	private final Class<T> klass;

	public FlagConverter(Class<T> klass) {
		this.klass = klass;
	}

	@Override
	public Integer convertToDatabaseColumn(EnumSet<T> enums) {
		int i = 0;
		for (T flag : enums) {
			i = Bit.set(i, flag.ordinal(), true);
		}

		return i;
	}

	@Override
	public EnumSet<T> convertToEntityAttribute(Integer flags) {
		EnumSet<T> out = EnumSet.noneOf(klass);
		for (T flag : klass.getEnumConstants()) {
			if (Bit.on(flags, flag.ordinal())) {
				out.add(flag);
			}
		}

		return out;
	}
}
