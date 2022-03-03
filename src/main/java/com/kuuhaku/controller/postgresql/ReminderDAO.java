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

import com.kuuhaku.model.persistent.Reminder;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class ReminderDAO {
	@SuppressWarnings("unchecked")
	public static List<Reminder> getExpiredReminders() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Reminder c WHERE c.nextReminder < :now ORDER BY c.nextReminder", Reminder.class);
		q.setParameter("now", ZonedDateTime.now(ZoneId.of("GMT-3")));

		try {
			return (List<Reminder>) q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Reminder> getReminders(String uid) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Reminder c WHERE c.uid = :uid ORDER BY c.nextReminder", Reminder.class);
		q.setParameter("uid", uid);

		try {
			return (List<Reminder>) q.getResultList();
		} finally {
			em.close();
		}
	}

	public static Reminder getReminder(String uid, int index) {
		return Helper.safeGet(getReminders(uid), index);
	}

	public static void removeReminder(Reminder r) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(em.contains(r) ? r : em.merge(r));
		em.getTransaction().commit();

		em.close();
	}

	public static void saveReminder(Reminder r) {
		if (r.expired()) {
			removeReminder(r);
			return;
		}

		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(r);
		em.getTransaction().commit();

		em.close();
	}
}
