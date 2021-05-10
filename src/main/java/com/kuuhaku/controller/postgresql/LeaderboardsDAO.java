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

import com.kuuhaku.model.persistent.Blacklist;
import com.kuuhaku.model.persistent.Leaderboards;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardsDAO {
	public static void submit(User u, Class<?> minigame, int score) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(new Leaderboards(u.getId(), u.getName(), minigame.getSimpleName(), score));
		em.getTransaction().commit();

		em.close();
	}

	public static int getUserScore(String id, Class<?> minigame) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT SUM(l.score)
				FROM Leaderboards l 
				WHERE l.uid = :uid 
				AND l.minigame = :minigame
				""", Blacklist.class);
		q.setParameter("uid", id);
		q.setParameter("minigame", minigame);
		q.setMaxResults(10);

		try {
			return ((Long) q.getSingleResult()).intValue();
		} catch (NoResultException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Leaderboards> getFaceoffLeaderboards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT MAX(l.id) AS id
					 , l.uid
					 , l.usr
					 , l.minigame
					 , MIN(l.score) AS score
				FROM Leaderboards l
				WHERE l.minigame = 'FaceoffCommand'
				GROUP BY l.uid, l.usr, l.minigame
				ORDER BY score, id
				""");
		q.setMaxResults(10);

		try {
			return ((List<Object[]>) q.getResultList()).stream()
					.map(o -> new Leaderboards((int) o[0], String.valueOf(o[1]), String.valueOf(o[2]), String.valueOf(o[3]), (int) o[4]))
					.collect(Collectors.toList());
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Leaderboards> getSlotsLeaderboards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT MAX(l.id) AS id
					 , l.uid
					 , l.usr
					 , l.minigame
					 , MAX(l.score) AS score
				FROM Leaderboards l
				WHERE l.minigame = 'SlotsCommand'
				GROUP BY l.uid, l.usr, l.minigame
				ORDER BY score DESC, id
				""");
		q.setMaxResults(10);

		try {
			return ((List<Object[]>) q.getResultList()).stream()
					.map(o -> new Leaderboards((int) o[0], String.valueOf(o[1]), String.valueOf(o[2]), String.valueOf(o[3]), (int) o[4]))
					.collect(Collectors.toList());
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Leaderboards> getCommonLeaderboards(Class<?> minigame) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT MAX(l.id) AS id
					 , l.uid
					 , l.usr
					 , l.minigame
					 , CAST(SUM(l.score) AS INTEGER) AS score
				FROM Leaderboards l
				WHERE l.minigame = :minigame
				GROUP BY l.uid, l.usr, l.minigame
				ORDER BY score DESC, id
				""");
		q.setParameter("minigame", minigame.getSimpleName());

		try {
			return ((List<Object[]>) q.getResultList()).stream()
					.map(o -> new Leaderboards((int) o[0], String.valueOf(o[1]), String.valueOf(o[2]), String.valueOf(o[3]), (int) o[4]))
					.collect(Collectors.toList());
		} finally {
			em.close();
		}
	}
}
