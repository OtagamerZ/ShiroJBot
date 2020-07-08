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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.model.persistent.FoilOffset;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

public class FoilOffsetDAO {
	public static Map<String, Integer> getOffsets() {
		EntityManager em = Manager.getEntityManager();

		try {
			return new HashMap<>() {{
				put("red", em.find(FoilOffset.class, "red").getValue());
				put("green", em.find(FoilOffset.class, "green").getValue());
				put("blue", em.find(FoilOffset.class, "blue").getValue());
			}};
		} finally {
			em.close();
		}
	}
}
