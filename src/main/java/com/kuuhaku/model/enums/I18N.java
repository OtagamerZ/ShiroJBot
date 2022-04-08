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

package com.kuuhaku.model.enums;

import com.kuuhaku.Main;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public enum I18N {
	PT, EN;

	private final Locale locale = new Locale(name().toLowerCase(Locale.ROOT));

	public static String get(I18N code, String key) {
		return code.get(key);
	}

	public static String get(I18N code, String key, Object... args) {
		return code.get(key, args);
	}

	public String get(String key) {
		if (key == null) return "";

		String lower = key.toLowerCase(Locale.ROOT);
		if (!key.equals(lower)) return get(lower);

		return Main.getCacheManager().getLocaleCache().computeIfAbsent(name() + "-" + key, k -> {
			try {
				String message = ResourceBundle.getBundle("locale/lang", locale).getString(key);
				String icon;
				try {
					icon = ResourceBundle.getBundle("locale/lang", locale).getString("icon/" + key.split("/")[0]);
				} catch (MissingResourceException e) {
					icon = "";
				}

				return icon.isBlank() ? message : icon + " | " + message;
			} catch (MissingResourceException e) {
				return key;
			}
		});
	}

	public String get(String key, Object... args) {
		if (key == null) return "";

		String lower = key.toLowerCase(Locale.ROOT);
		if (!key.equals(lower)) return get(lower, args);

		return Main.getCacheManager().getLocaleCache().computeIfAbsent(name() + "-" + key, k -> {
			try {
				String message = ResourceBundle.getBundle("locale/lang", locale).getString(key);
				String icon;
				try {
					icon = ResourceBundle.getBundle("locale/lang", locale).getString("icon/" + key.split("/")[0]);
				} catch (MissingResourceException e) {
					icon = "";
				}

				return icon.isBlank() ? message : icon + " | " + message;
			} catch (MissingResourceException e) {
				return key;
			}
		}).formatted(args);
	}
}
