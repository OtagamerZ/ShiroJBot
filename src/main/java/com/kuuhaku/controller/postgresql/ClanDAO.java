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

import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.model.persistent.ClanMember;
import com.kuuhaku.model.records.ClanRanking;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

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

		Query q = em.createQuery("SELECT c FROM Clan c JOIN c.members m WHERE m.uid = :id", Clan.class);
		q.setParameter("id", id);

		try {
			return (Clan) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
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
		em.createQuery("DELETE FROM Clan c WHERE c.id = :id")
				.setParameter("id", clan.getId())
				.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Clan> getUnpaidClans() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Clan c", Clan.class);

		try {
			return ((Stream<Clan>) q.getResultStream()).filter(c -> !c.hasPaidRent()).toList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<ClanRanking> getClanRanking() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT c.id, c.name, c.score, c.rank, c.icon FROM shiro.\"GetClanRanking\" c");
		q.setMaxResults(10);

		try {
			return Helper.map(ClanRanking.class, q.getResultList());
		} finally {
			em.close();
		}
	}

	public static ClanRanking getClanChampion() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT c.id, c.name, c.score, c.rank, c.icon FROM shiro.\"GetClanChampion\" c");
		q.setMaxResults(1);

		try {
			return Helper.map(ClanRanking.class, (Object[]) q.getSingleResult());
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static ClanRanking getClanPosition(int id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT c.id, c.name, c.score, c.rank, c.icon FROM shiro.\"GetClanRanking\" c WHERE c.id = :id");
		q.setParameter("id", id);

		try {
			return Helper.map(ClanRanking.class, (Object[]) q.getSingleResult());
		} finally {
			em.close();
		}
	}

	public static ClanMember getClanMember(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM ClanMember m WHERE m.uid = :id", ClanMember.class);
		q.setParameter("id", id);

		try {
			return (ClanMember) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static void saveMember(ClanMember member) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(member);
		em.getTransaction().commit();

		em.close();
	}

	public static void resetScores() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("UPDATE ClanMember SET score = 0").executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
