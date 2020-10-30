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

import com.kuuhaku.model.persistent.Lobby;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class LobbyDAO {
	public static Lobby getLobby(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Lobby.class, id);
		} finally {
			em.close();
		}
	}

	public static Lobby getLobby(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createQuery("SELECT l FROM Lobby l WHERE l.owner = :owner", Lobby.class);
			q.setParameter("owner", id);
			return (Lobby) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Lobby> getLobbies() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT l FROM Lobby l WHERE SIZE(l.players) < l.maxPlayers ORDER BY l.id", Lobby.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static void saveLobby(Lobby lb) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(lb);
		em.getTransaction().commit();

		em.close();
	}

	public static void deleteLobby(Lobby lb) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("DELETE FROM Lobby l WHERE l.id = :id");
		q.setParameter("id", lb.getId());

		em.getTransaction().begin();
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
