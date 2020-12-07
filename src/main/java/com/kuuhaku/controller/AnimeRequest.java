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

package com.kuuhaku.controller;

import com.kuuhaku.utils.Helper;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class AnimeRequest {
	public static JSONObject getData(String anime, String query) {
		JSONObject json = new JSONObject() {{
			put("query", query);
			put("variables", new JSONObject() {{
				put("anime", anime);
			}});
		}};

		return Helper.post("https://graphql.anilist.co", json, Map.of(
				"Content-Type", "application/json; charset=UTF-8",
				"Accept", "application/json",
				"User-Agent", "Mozilla/5.0"
				)
		);
	}

	public static JSONObject getNAData(String name) {
		JSONObject resposta;
		if (System.getenv().containsKey("NOWANIMES_URL") && System.getenv().containsKey("NOWANIMES_TOKEN")) {
			resposta = Helper.get(System.getenv("NOWANIMES_URL"),
					new JSONObject() {{
						put("anime", name);
					}},
					Collections.emptyMap(),
					System.getenv("NOWANIMES_TOKEN")
			);
		} else {
			return new JSONObject();
		}

		Helper.logger(AnimeRequest.class).debug(resposta);
		return resposta;
	}

	public static JSONObject getMHData(String name) {
		JSONObject resposta;
		if (System.getenv().containsKey("MEGAHENTAIS_URL") && System.getenv().containsKey("MEGAHENTAIS_TOKEN")) {
			resposta = Helper.get(System.getenv("MEGAHENTAIS_URL"),
					new JSONObject() {{
						put("anime", name);
					}},
					Collections.emptyMap(),
					System.getenv("MEGAHENTAIS_TOKEN")
			);
		} else {
			return new JSONObject();
		}

		Helper.logger(AnimeRequest.class).debug(resposta);
		return resposta;
	}
}
