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

import com.kuuhaku.model.enums.StaffType;
import com.kuuhaku.model.persistent.Staff;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;

public class StaffDAO {
	public static Staff getUser(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Staff t = em.find(Staff.class, id);
			if (t == null) {
				return new Staff();
			} else return t;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Staff> getStaff() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT s FROM Staff s", Staff.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Staff> getStaff(StaffType type) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT s FROM Staff s WHERE s.type IN :all", Staff.class);
		if (type == StaffType.DEVELOPER) {
			q.setParameter("all", Set.of(type));
		} else {
			q.setParameter("all", Set.of(type, StaffType.OVERSEER, StaffType.DEVELOPER));
		}

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}
