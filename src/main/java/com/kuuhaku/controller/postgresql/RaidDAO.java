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

import com.kuuhaku.model.persistent.RaidInfo;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class RaidDAO {
	public static RaidInfo getRaid(int id, String sid) {
		EntityManager em = Manager.getEntityManager();

		try {
			RaidInfo r = em.find(RaidInfo.class, id);
			if (r != null && r.getSid().equals(sid)) {
				return r;
			}

			return null;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<RaidInfo> getRaids(String sid) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT r FROM RaidInfo r WHERE r.sid = :sid ORDER BY r.id", RaidInfo.class);
		q.setParameter("sid", sid);

		try {
			return (List<RaidInfo>) q.getResultList();
		} finally {
			em.close();
		}
	}

	public static int getUserRaids(String uid) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT COUNT(1) FROM RaidMember r WHERE r.uid = :uid");
		q.setParameter("uid", uid);

		try {
			return ((Number) q.getSingleResult()).intValue();
		} finally {
			em.close();
		}
	}

	public static void saveInfo(RaidInfo r) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(r);
		em.getTransaction().commit();

		em.close();
	}
}
