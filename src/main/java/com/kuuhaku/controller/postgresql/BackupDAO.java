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

import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.model.persistent.*;
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class BackupDAO {
	public static void dumpData(DataDump data) {
		EntityManager em = Manager.getEntityManager();
		em.getTransaction().begin();

		for (int i = 0; i < data.getCaDump().size(); i++) {
			em.merge(data.getCaDump().get(i));
			if (i % 20 == 0) {
				em.flush();
				em.clear();
			}
			if (i % 1000 == 0) {
				em.getTransaction().commit();
				em.clear();
				em.getTransaction().begin();
			}
		}

		for (int i = 0; i < data.getGcDump().size(); i++) {
			em.merge(data.getGcDump().get(i));
			if (i % 20 == 0) {
				em.flush();
				em.clear();
			}
			if (i % 1000 == 0) {
				em.getTransaction().commit();
				em.clear();
				em.getTransaction().begin();
			}
		}

		for (int i = 0; i < data.getmDump().size(); i++) {
			em.merge(data.getmDump().get(i));
			if (i % 20 == 0) {
				em.flush();
				em.clear();
			}
			if (i % 1000 == 0) {
				em.getTransaction().commit();
				em.clear();
				em.getTransaction().begin();
			}
		}

		for (int i = 0; i < data.getAuDump().size(); i++) {
			em.merge(data.getAuDump().get(i));
			if (i % 20 == 0) {
				em.flush();
				em.clear();
			}
			if (i % 1000 == 0) {
				em.getTransaction().commit();
				em.clear();
				em.getTransaction().begin();
			}
		}

		for (int i = 0; i < data.getKgDump().size(); i++) {
			em.merge(data.getKgDump().get(i));
			if (i % 20 == 0) {
				em.flush();
				em.clear();
			}
			if (i % 1000 == 0) {
				em.getTransaction().commit();
				em.clear();
				em.getTransaction().begin();
			}
		}

		em.getTransaction().commit();
		em.close();
	}

	@SuppressWarnings("unchecked")
	public static DataDump getData() {
		EntityManager em = Manager.getEntityManager();

		Query ca = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
		Query m = em.createQuery("SELECT m FROM Member m", Member.class);
		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		Query au = em.createQuery("SELECT u FROM AppUser u", AppUser.class);
		Query kg = em.createQuery("SELECT k FROM Kawaigotchi k", Kawaigotchi.class);
		DataDump dump = new DataDump(ca.getResultList(), m.getResultList(), gc.getResultList(), au.getResultList(), kg.getResultList());
		em.close();

		return dump;
	}

	public static Backup getGuildBackup(Guild g) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM Backup b WHERE b.guild LIKE :id", Backup.class);
		q.setParameter("id", g.getId());

		try {
			return (Backup) q.getSingleResult();
		} catch (NoResultException e) {
			return new Backup();
		} finally {
			em.close();
		}
	}

	public static void saveBackup(Backup b) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(b);
		em.getTransaction().commit();

		em.close();
	}
}
