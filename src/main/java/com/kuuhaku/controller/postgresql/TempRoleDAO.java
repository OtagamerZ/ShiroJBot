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

import com.kuuhaku.model.persistent.TempRole;
import com.kuuhaku.model.persistent.id.CompositeRoleId;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class TempRoleDAO {
	public static TempRole getTempRole(String id, String guild, String role) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(TempRole.class, new CompositeRoleId(id, guild, role));
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<TempRole> getAllRoles() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT r FROM TempRole r", TempRole.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<TempRole> getRolesByGuild(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT r FROM TempRole r WHERE sid = :sid", TempRole.class);
		q.setParameter("sid", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<TempRole> getRolesByUser(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT r FROM TempRole r WHERE uid = :uid", TempRole.class);
		q.setParameter("uid", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<TempRole> getRolesByGuildAndUser(String id, String guild) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT r FROM TempRole r WHERE uid = :uid AND sid = :sid", TempRole.class);
		q.setParameter("uid", id);
		q.setParameter("sid", guild);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<TempRole> getExpiredRoles() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT r FROM TempRole r WHERE until <= :today", TempRole.class);
		q.setParameter("today", ZonedDateTime.now(ZoneId.of("GMT-3")));

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static void saveTempRole(TempRole tr) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(tr);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeTempRole(TempRole tr) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("DELETE FROM TempRole r WHERE uid = :uid AND sid = :sid AND rid = :rid")
				.setParameter("uid", tr.getUid())
				.setParameter("sid", tr.getSid())
				.setParameter("rid", tr.getRid())
				.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
