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

import com.kuuhaku.model.persistent.Trophy;

import javax.persistence.EntityManager;

public class TrophyDAO {
	public static Trophy getTrophies(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Trophy t = em.find(Trophy.class, id);
			if (t == null) saveTrophies(new Trophy(id));
			return getTrophies(id);
		} finally {
			em.close();
		}
	}

	public static void removeTrophies(Trophy t) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void saveTrophies(Trophy t) {
		if (BlacklistDAO.isBlacklisted(t.getUid())) return;
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}
}
