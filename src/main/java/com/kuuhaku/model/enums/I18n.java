/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.utils.ShiroInfo;

import java.text.MessageFormat;
import java.util.Locale;

public enum I18n {
	PT(new Locale("pt")),
	EN(new Locale("en")),
	ES(new Locale("es"));

	private final Locale locale;

	I18n(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return locale;
	}

	public static String getString(String key, Object... params) {
		String prefix;
		if (key.contains("_"))
			prefix = key.split("_")[0];
		else
			prefix = "misc";

		if (params.length > 0)
			return MessageFormat.format(ShiroInfo.getLocale(PT, prefix).getString(key), params);
		else
			return ShiroInfo.getLocale(PT, prefix).getString(key);
	}
}
