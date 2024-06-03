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

package com.kuuhaku.schedule;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.PreInitialize;
import com.kuuhaku.interfaces.annotations.Schedule;
import com.ygimenez.json.JSONArray;
import kotlin.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Schedule("* * * * *")
public class MinuteSchedule implements Runnable, PreInitialize {
	public static final Map<String, Pair<Integer, Long>> XP_TO_ADD = new ConcurrentHashMap<>();

	@Override
	public void run() {
		Map<String, Pair<Integer, Long>> xps = Map.copyOf(XP_TO_ADD);
		XP_TO_ADD.clear();

		JSONArray ja = new JSONArray();
		for (Map.Entry<String, Pair<Integer, Long>> e : xps.entrySet()) {
			String[] keys = e.getKey().split("-");
			if (keys.length != 2) continue;

			ja.add(Map.of(
					"uid", keys[0],
					"gid", keys[1],
					"xp", e.getValue().getFirst()
			));
		}

		DAO.applyNative("""
				UPDATE profile
				SET xp = xp + cast(vals -> 'xp' AS INT)
				FROM jsonb_array_elements(cast(?1 AS JSONB)) AS vals
				WHERE uid = vals ->> 'uid'
				  AND gid = vals ->> 'gid'
				""", ja.toString());
	}
}
