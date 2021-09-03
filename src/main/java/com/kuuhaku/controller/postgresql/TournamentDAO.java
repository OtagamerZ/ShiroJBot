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

import com.kuuhaku.model.persistent.tournament.Tournament;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class TournamentDAO {
	public static void save(Tournament t) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Tournament> getTournaments() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tournament t", Tournament.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Tournament> getTournaments(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tournament t JOIN t.participants p WHERE p.uid = :id AND NOT t.closed", Tournament.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Tournament> getOpenTournaments() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tournament t WHERE NOT t.closed", Tournament.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static Tournament getUserTournament(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tournament t JOIN t.participants p WHERE p.uid = :id AND t.closed AND NOT t.finished", Tournament.class);
		q.setParameter("id", id);

		try {
			return (Tournament) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Tournament getTournament(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Tournament.class, id);
		} finally {
			em.close();
		}
	}
}
