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

package com.kuuhaku.schedule;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.annotations.Schedule;

@Schedule("0 0 0 1/1 * ? *")
public class DailySchedule implements Runnable {
	@Override
	public void run() {
		DAO.applyNative("""
				INSERT INTO aux.card_counter (anime_id, count)
				SELECT vcc.anime_id
				     , vcc.count
				FROM aux.v_card_counter vcc
				ON CONFLICT DO UPDATE
				    SET count = vcc.count
				""");
		DAO.applyNative("""
				INSERT INTO aux.collection_counter (uid, anime_id, normal, chrome)
				SELECT vcc.kawaipon_uid
				  , vcc.anime_id
				  , vcc.normal
				  , vcc.chrome
				FROM aux.v_collection_counter vcc
				ON CONFLICT DO UPDATE
					SET normal = vcc.normal
					  , chrome = vcc.chrome
				""");
	}
}
