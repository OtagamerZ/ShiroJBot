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

import com.kuuhaku.Main;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.persistent.MatchMakingRating;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MatchMakingRatingDAO {
	public static MatchMakingRating getMMR(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT mmr FROM MatchMakingRating mmr WHERE uid = :id", MatchMakingRating.class);
		q.setParameter("id", id);

		try {
			return (MatchMakingRating) q.getSingleResult();
		} catch (NoResultException e) {
			User u = Main.getInfo().getUserByID(id);
			MatchMakingRating mmr = new MatchMakingRating(u.getId());
			saveMMR(mmr);
			return mmr;
		} finally {
			em.close();
		}
	}

	public static void saveMMR(MatchMakingRating mmr) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(mmr);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeMMR(MatchMakingRating mmr) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(mmr);
		em.getTransaction().commit();

		em.close();
	}

	public static void resetRanks() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Query q = em.createQuery("UPDATE MatchMakingRating mmr SET mmr.mmr = mmr.mmr / 2, mmr.tier = :tier, mmr.rankPoints = 0");
		q.setParameter("tier", RankedTier.UNRANKED);
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<MatchMakingRating> getMMRRank() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT mmr FROM MatchMakingRating mmr ORDER BY mmr.mmr DESC", MatchMakingRating.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<MatchMakingRating> getMMRRank(int tier) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT mmr FROM MatchMakingRating mmr ORDER BY mmr.rankPoints DESC, mmr.promWins + mmr.promLosses DESC", MatchMakingRating.class);

		try {
			return (List<MatchMakingRating>) q.getResultStream()
					.filter(r -> ((MatchMakingRating) r).getTier().getTier() == tier)
					.collect(Collectors.toList());
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<MatchMakingRating> getMMRRank(RankedTier tier) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT mmr FROM MatchMakingRating mmr WHERE mmr.tier = :tier ORDER BY mmr.rankPoints DESC, mmr.promWins + mmr.promLosses DESC", MatchMakingRating.class);
		q.setParameter("tier", tier);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static double getAverageMMR(RankedTier tier) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT AVG(mmr.mmr) AS average FROM MatchMakingRating mmr WHERE mmr.tier = :tier GROUP BY mmr.tier ORDER BY average");
		q.setParameter("tier", tier);

		try {
			return (Double) q.getSingleResult();
		} catch (NoResultException e) {
			return 0;
		} finally {
			em.close();
		}
	}
}
