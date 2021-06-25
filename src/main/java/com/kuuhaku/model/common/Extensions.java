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

package com.kuuhaku.model.common;

import com.github.kevinsawicki.http.HttpRequest;
import com.kuuhaku.utils.Helper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Extensions {
	private static final String[] list;

	static {
		String[] tldList;
		try {
			HttpRequest iana = HttpRequest.get("http://data.iana.org/TLD/tlds-alpha-by-domain.txt", true)
					.header("Content-Type", "application/json; charset=UTF-8")
					.header("User-Agent", "Mozilla/5.0");

			List<String> tlds = new ArrayList<>();
			LineIterator list = IOUtils.lineIterator(iana.stream(), StandardCharsets.UTF_8);
			while (list.hasNext()) {
				String line = list.next();
				if (!line.startsWith("#"))
					tlds.add("." + line.toLowerCase(Locale.ROOT));

			}

			tldList = tlds.toArray(String[]::new);
		} catch (IOException e) {
			tldList = new String[0];
		}

		list = tldList;
	}

	public static boolean checkExtension(String str) {
		return Helper.containsAny(str, list);
	}
}
