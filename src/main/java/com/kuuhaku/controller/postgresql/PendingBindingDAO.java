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

import com.kuuhaku.model.persistent.PendingBinding;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class PendingBindingDAO {
	public static PendingBinding getPendingBinding(String hash) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT p FROM PendingBinding p WHERE hash = :hash", PendingBinding.class);
		q.setParameter("hash", hash);

		try {
			return (PendingBinding) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static void savePendingBinding(PendingBinding pb) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(pb);
		em.getTransaction().commit();

		em.close();
	}

	public static void removePendingBinding(PendingBinding pb) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(pb);
		em.getTransaction().commit();

		em.close();
	}
}
