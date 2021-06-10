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

import com.kuuhaku.model.persistent.CustomAnswer;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;
import java.util.Locale;

public class CustomAnswerDAO {
	@SuppressWarnings("unchecked")
	public static CustomAnswer getCAByTrigger(String trigger, String guild) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT c.id
				     , c.guildId
				     , c.trigger
				     , c.answer
				     , c.anywhere
				     , c.chance
				FROM CustomAnswer c 
				WHERE guildId = :guild
				AND (
					(c.anywhere AND :trigger LIKE LOWER('%'||trigger||'%'))
					OR LOWER(trigger) = :trigger
				)
				""");
		q.setParameter("trigger", trigger.toLowerCase(Locale.ROOT));
		q.setParameter("guild", guild);

		try {
			List<Object[]> answers = q.getResultList();

			if (answers.isEmpty()) return null;
			return Helper.map(CustomAnswer.class, Helper.getRandomN(answers, 1).get(0));
		} finally {
			em.close();
		}
	}

	public static CustomAnswer getCAByID(int id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswer c WHERE id = :id", CustomAnswer.class);
		q.setParameter("id", id);

		try {
			return (CustomAnswer) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

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

		Query q = em.createQuery("SELECT c FROM CustomAnswer c WHERE guildId = :guild", CustomAnswer.class);
		q.setParameter("guild", id);

		try {
			return q.getResultList();
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
