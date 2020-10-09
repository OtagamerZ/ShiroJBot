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

import com.kuuhaku.model.persistent.GuildConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class GuildDAO {
	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuilds() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		List<GuildConfig> gcs = (List<GuildConfig>) gc.getResultList();

		em.close();

		return gcs;
	}
}
