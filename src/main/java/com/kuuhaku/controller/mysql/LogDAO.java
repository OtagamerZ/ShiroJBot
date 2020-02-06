/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.persistent.Log;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class LogDAO {
	public static void saveLog(Log log) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(log);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Object[]> getUsage() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT guild, uses, lastused FROM GetUsage");

		return q.getResultList();
	}
}
