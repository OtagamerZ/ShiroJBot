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

import com.kuuhaku.model.persistent.AppUser;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class DashboardDAO {
	public static AppUser getData(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT u FROM AppUser u WHERE uid LIKE :id", AppUser.class);
		q.setParameter("id", id);

		try {
			return (AppUser) q.getSingleResult();
		} finally {
			em.close();
		}
	}

	public static boolean isRegistered(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT u FROM AppUser u WHERE uid LIKE :id AND login IS NOT NULL AND password IS NOT NULL");
		q.setParameter("id", id);

		try {
			return q.getResultList().size() > 0;
		} finally {
			em.close();
		}
	}

	public static String auth(String login, String pass) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT uid FROM AppUser u WHERE login LIKE :login AND password LIKE :pass");
		q.setParameter("login", login);
		q.setParameter("pass", pass);

		try {
			return "{\"id\": \"" + q.getSingleResult() + "\"}";
		} finally {
			em.close();
		}
	}

	public static void saveData(AppUser u) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(u);
		em.getTransaction().commit();

		em.close();
	}
}
