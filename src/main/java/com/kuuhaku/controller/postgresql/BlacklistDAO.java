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

import com.github.twitch4j.common.events.domain.EventUser;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Blacklist;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class BlacklistDAO {
	public static void blacklist(Blacklist bl) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(bl);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeFromList(Blacklist bl) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(bl);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Blacklist> getBlacklist() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM Blacklist b", Blacklist.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static boolean isBlacklisted(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM Blacklist b WHERE uid = :id", Blacklist.class);
		q.setParameter("id", id);

		try {
			q.getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} finally {
			em.close();
		}
	}

	public static boolean isBlacklisted(User user) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM Blacklist b WHERE uid = :id", Blacklist.class);
		q.setParameter("id", user.getId());

		try {
			q.getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} finally {
			em.close();
		}
	}

	public static boolean isBlacklisted(EventUser user) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a WHERE twitchId = :id", Account.class);
		q.setParameter("id", user.getId());

		Account acc;
		try {
			acc = (Account) q.getSingleResult();
		} catch (NoResultException e) {
			acc = null;
		}

		boolean blacklisted = false;
		if (acc != null) {
			q = em.createQuery("SELECT b FROM Blacklist b WHERE uid = :id", Blacklist.class);
			q.setParameter("id", acc.getUid());

			try {
				q.getSingleResult();
				return true;
			} catch (NoResultException e) {
				return false;
			} finally {
				em.close();
			}
		} else {
			em.close();
			return false;
		}
	}
}
