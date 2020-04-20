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

package com.kuuhaku.controller.sqlite;

import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.model.persistent.AppUser;
import com.kuuhaku.model.persistent.CustomAnswers;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.utils.Helper;

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
			data.getKgDump().forEach(em::merge);
			em.getTransaction().commit();

			return true;
		} catch (Exception e) {
			Helper.logger(BackupDAO.class).error(e + " | " + e.getStackTrace()[0]);
			return false;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CustomAnswers> getCADump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m", Member.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getGuildDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<AppUser> getAppUserDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT u FROM AppUser u", AppUser.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Kawaigotchi> getKawaigotchiDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT k FROM Kawaigotchi k", Kawaigotchi.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}
