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

import com.kuuhaku.model.persistent.GuildBuff;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class GuildBuffDAO {
	@SuppressWarnings("unchecked")
	public static List<GuildBuff> getAllBuffs() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT gb FROM GuildBuff gb", GuildBuff.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static GuildBuff getBuffs(String guild) {
		EntityManager em = Manager.getEntityManager();

		try {
			GuildBuff gb = em.find(GuildBuff.class, guild);
			if (gb == null) return new GuildBuff(guild);
			else return gb;
		} finally {
			em.close();
		}
	}

	public static void saveBuffs(GuildBuff gb) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(gb);
		em.getTransaction().commit();

		em.close();
	}
}
