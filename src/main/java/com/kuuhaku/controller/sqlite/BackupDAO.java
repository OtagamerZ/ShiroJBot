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

package com.kuuhaku.controller.sqlite;

import com.kuuhaku.model.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class BackupDAO {
	public static boolean restoreData(DataDump data) {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();
			data.getCaDump().forEach(em::merge);
			data.getmDump().forEach(em::merge);
			data.getGcDump().forEach(em::merge);
			data.getAuDump().forEach(em::merge);
			em.getTransaction().commit();

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CustomAnswers> getCADump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
		List<CustomAnswers> ca = q.getResultList();

		return ca;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m", Member.class);

		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getGuildDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);

		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<AppUser> getAppUserDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT u FROM AppUser u", AppUser.class);

		return q.getResultList();
	}
}
