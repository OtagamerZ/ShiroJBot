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

import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class KGotchiDAO {
	public static Kawaigotchi getKawaigotchi(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT k FROM Kawaigotchi k WHERE k.userId = :id", Kawaigotchi.class);
		q.setParameter("id", id);

		try {
			return (Kawaigotchi) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static void saveKawaigotchi(Kawaigotchi k) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(k);
		em.getTransaction().commit();

		em.close();
	}

	public static void deleteKawaigotchi(Kawaigotchi k) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(k);
		em.getTransaction().commit();

		em.close();
	}
}
