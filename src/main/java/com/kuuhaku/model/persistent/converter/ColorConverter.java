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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.awt.*;

@Converter(autoApply = true)
public class ColorConverter implements AttributeConverter<Color, String> {
	@Override
	public String convertToDatabaseColumn(Color color) {
		return "%06X".formatted(0x00FFFFFF & color.getRGB());
	}

	@Override
	public Color convertToEntityAttribute(String hex) {
		if (hex == null) return new Color(0);

		return switch (hex.length()) {
			case 6 -> new Color(Integer.parseInt(hex, 16));
			case 8 -> new Color(Integer.parseInt(hex.substring(2), 16));
			default -> new Color(0);
		};
	}
}
