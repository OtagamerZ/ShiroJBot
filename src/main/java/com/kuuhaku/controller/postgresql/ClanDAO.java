/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.persistent.Clan;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;
import java.util.Locale;

public class ClanDAO {
	public static Clan getClan(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Clan c WHERE LOWER(c.name) = :name", Clan.class);
		q.setParameter("name", name.toLowerCase(Locale.ROOT));

		try {
			return (Clan) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Clan getUserClan(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m.clan FROM ClanMember m WHERE m.uid = :id", Clan.class);
		q.setParameter("id", id);

		try {
			return (Clan) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static boolean isMember(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m.clan FROM ClanMember m WHERE m.uid = :id", Clan.class);
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

	public static void saveClan(Clan clan) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(clan);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeClan(Clan clan) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(em.contains(clan) ? clan : em.merge(clan));
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Clan> getUnpaidClans() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Clan c WHERE EXTRACT(MONTH FROM c.paidRent) <> EXTRACT(MONTH FROM CURRENT_DATE)", Clan.class);

		try {
			return (List<Clan>) q.getResultList();
		} finally {
			em.close();
		}
	}
}
