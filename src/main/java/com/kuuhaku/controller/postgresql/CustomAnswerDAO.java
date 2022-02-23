/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.persistent.CustomAnswer;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class CustomAnswerDAO {
	public static CustomAnswer getCAByIDAndGuild(int id, String guild) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswer c WHERE id = :id AND guildId = :guild", CustomAnswer.class);
		q.setParameter("id", id);
		q.setParameter("guild", guild);

		try {
			return (CustomAnswer) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CustomAnswer> getCAByGuild(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswer c WHERE guildId = :guild ORDER BY c.id", CustomAnswer.class);
		q.setParameter("guild", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CustomAnswer> getCustomAnswers() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswer c", CustomAnswer.class);

		try {
			return (List<CustomAnswer>) q.getResultList();
		} finally {
			em.close();
		}
	}

	public static void addCustomAnswer(CustomAnswer ca) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(ca);
		em.getTransaction().commit();

		em.close();
	}

	public static void deleteCustomAnswer(CustomAnswer ca) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(em.contains(ca) ? ca : em.merge(ca));
		em.getTransaction().commit();

		em.close();
	}
}
