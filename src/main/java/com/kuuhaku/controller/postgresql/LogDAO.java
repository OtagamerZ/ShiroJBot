/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.persistent.Log;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LogDAO {
	public static void saveLog(Log log) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(log);
		em.createQuery("DELETE FROM Log l WHERE l.guildId NOT IN (SELECT g.guildId FROM GuildConfig g)").executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings({"unchecked", "SqlResolve"})
	public static List<Object[]> getUsage() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT * FROM shiro.\"GetUsage\"");

		try {
			return (List<Object[]>) q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings({"SqlResolve"})
	public static String getUsername(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT l.usr
				FROM Log l
				WHERE l.uid = :id
				ORDER BY l.id DESC
				""");
		q.setParameter("id", id);
		q.setMaxResults(1);

		try {
			return (String) q.getSingleResult();
		} catch (NoResultException e) {
			return "???";
		} finally {
			em.close();
		}
	}

	@SuppressWarnings({"unchecked", "SqlResolve"})
	public static List<Object[]> auditUser(String id, String type) {
		EntityManager em = Manager.getEntityManager();

		try {
			return switch (type.toUpperCase(Locale.ROOT)) {
				case "T" -> {
					Query q = em.createNativeQuery("""
							SELECT CASE t.fromclass WHEN '' THEN 'Anonymous' ELSE t.fromclass END
								 , SUM(t.value)
							FROM transaction t
							WHERE t.uid = :id
							GROUP BY t.fromclass
							ORDER BY 2 DESC
							""");
					q.setParameter("id", id);

					yield (List<Object[]>) q.getResultList();
				}
				case "C" -> {
					Query q = em.createNativeQuery("""
							SELECT l.command
								 , COUNT(1)
							FROM logs l
							WHERE l.uid = :id
							GROUP BY l.command
							ORDER BY 2 DESC
							""");
					q.setParameter("id", id);

					yield (List<Object[]>) q.getResultList();
				}
				default -> new ArrayList<>();
			};
		} finally {
			em.close();
		}
	}
}
