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

import com.kuuhaku.model.persistent.Checklist;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class ChecklistDAO {
	@SuppressWarnings("unchecked")
	public static List<Checklist> getChecklists(String uid) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Checklist c WHERE c.uid = :uid", Checklist.class);
		q.setParameter("uid", uid);

		try {
			return (List<Checklist>) q.getResultList();
		} finally {
			em.close();
		}
	}

	public static Checklist getChecklist(String uid, int index) {
		return Helper.safeGet(getChecklists(uid), index);
	}

	public static void removeChecklist(Checklist c) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(em.contains(c) ? c : em.merge(c));
		em.getTransaction().commit();

		em.close();
	}

	public static void saveChecklist(Checklist c) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(c);
		em.getTransaction().commit();

		em.close();
	}
}
