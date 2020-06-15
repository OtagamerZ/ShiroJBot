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

package com.kuuhaku.controller.sqlite;

import com.kuuhaku.controller.postgresql.Manager;
import com.kuuhaku.model.persistent.Blacklist;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class BlacklistDAO {
	public static void blacklist(Blacklist bl) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(bl);
		em.getTransaction().commit();

		em.close();
	}

	public static boolean isBlacklisted(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM Blacklist b WHERE id LIKE :id", Blacklist.class);
		q.setParameter("id", id);

		try {
			q.getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} finally {
			em.close();
		}
	}
}
