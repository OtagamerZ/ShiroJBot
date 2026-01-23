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

package com.kuuhaku.model.enums;

import com.kuuhaku.Main;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.text.Uwuifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;

public enum I18N {
	PT(ZoneId.of("GMT-3"), "🇧🇷"),
	EN(ZoneId.of("GMT-4"), "🇺🇸"),

	UWU_PT(PT),
	UWU_EN(EN),
	;

	private final Locale locale = Locale.forLanguageTag(name().toLowerCase());
	private final ZoneId zone;
	private final String emoji;
	private final I18N parent;

	I18N(ZoneId zone, String emoji) {
		this.zone = zone;
		this.emoji = emoji;
		this.parent = this;
	}

	I18N(I18N parent) {
		this.zone = parent.zone;
		this.emoji = parent.emoji;
		this.parent = parent;
	}

	public static String get(I18N code, String key) {
		return code.get(key);
	}

	public static String get(I18N code, String key, Object... args) {
		return code.get(key, args);
	}

	public String get(String key, Object... args) {
		if (key == null) return "";
		else if (args == null) return parent.get(key);
		else if (key.contains(" ") && !key.contains("/")) return key;

		String lower = key.toLowerCase();
		if (!key.equals(lower)) return parent.get(lower, args);

		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (StringUtils.isNumeric(String.valueOf(arg))) {
				args[i] = separate(arg);
			}
		}

		String out = Main.getCacheManager().computeLocale(parent.name() + "-" + key, (k, v) -> {
			if (v != null) return v;

			try {
				String message = ResourceBundle.getBundle("locale/lang", parent.locale).getString(key);
				String icon;
				try {
					icon = ResourceBundle.getBundle("locale/lang", parent.locale).getString("icon/" + key.split("/")[0]);
				} catch (MissingResourceException e) {
					icon = "";
				}

				if (!icon.isBlank()) {
					Matcher m = Utils.regex(message, "^#{1,3} (?=.*)");
					if (m.find()) {
						String md = m.group();
						return md + icon + " | " + message.substring(md.length());
					}
				}

				return icon.isBlank() ? message : icon + " | " + message;
			} catch (MissingResourceException e) {
				return key;
			}
		}).formatted(args);

		if (isUwu()) {
			return Uwuifier.INSTANCE.uwu(parent, out);
		}

		return out;
	}

	public ZoneId getZone() {
		return zone;
	}

	public String getEmoji() {
		return emoji;
	}

	public I18N getParent() {
		return parent;
	}

	public boolean is(I18N locale) {
		return parent == locale.parent;
	}

	public boolean isUwu() {
		return name().startsWith("UWU");
	}

	public String separate(Object value) {
		try {
			Number n = value instanceof Number nb ? nb : NumberUtils.createNumber(String.valueOf(value));
			return DecimalFormat.getInstance(locale).format(n);
		} catch (NumberFormatException e) {
			return String.valueOf(value);
		}
	}

	public static I18N[] validValues() {
		return Arrays.stream(I18N.values())
				.filter(l -> l.parent == l)
				.toArray(I18N[]::new);
	}
}
