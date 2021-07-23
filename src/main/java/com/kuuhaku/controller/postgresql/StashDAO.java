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

import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Stash;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class StashDAO {
	@SuppressWarnings("unchecked")
	public static List<Stash> getCards(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT s FROM Stash s WHERE owner = :id", Stash.class);
		q.setParameter("id", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static int getRemainingSpace(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT a.cardStashCapacity - COUNT(s)
				FROM Stash s
				INNER JOIN Account a ON a.uid = s.owner
				WHERE s.owner = :id
				""");
		q.setParameter("id", id);

		try {
			return ((Number) q.getSingleResult()).intValue();
		} finally {
			em.close();
		}
	}

	public static Stash getCard(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Stash s = em.find(Stash.class, id);
			if (s != null) removeCard(s);

			return s;
		} finally {
			em.close();
		}
	}

	public static void saveCard(Stash card) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(card);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeCard(Stash card) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(card);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Stash> getStashedCards(String name, KawaiponRarity rarity, String anime, boolean foil, boolean onlyKp, boolean onlyEq, boolean onlyFd, String owner) {
		EntityManager em = Manager.getEntityManager();

		String query = """
				SELECT s
				FROM Stash s
				JOIN s.card c
				JOIN c.anime a
				WHERE s.owner = :owner
				%s
				""";

		String[] params = {
				name != null ? "AND c.id LIKE UPPER(:name)" : "",
				rarity != null ? "AND c.rarity LIKE UPPER(:rarity)" : "",
				anime != null ? "AND a.id LIKE UPPER(:anime)" : "",
				foil ? "AND m.foil = :foil" : "",
				onlyKp ? "AND c.rarity <> 'EQUIPMENT' AND c.rarity <> 'FIELD'" : "",
				onlyEq ? "AND c.rarity = 'EQUIPMENT'" : "",
				onlyFd ? "AND c.rarity = 'FIELD'" : "",
				"ORDER BY s.foil DESC, c.rarity DESC, a.id, c.id"
		};

		Query q = em.createQuery(query.formatted(String.join("\n", params)), Stash.class);
		q.setParameter("owner", owner);

		if (!params[0].isBlank()) q.setParameter("name", "%" + name + "%");
		if (!params[1].isBlank()) q.setParameter("rarity", rarity.name());
		if (!params[2].isBlank()) q.setParameter("anime", "%" + anime + "%");
		if (!params[3].isBlank()) q.setParameter("foil", foil);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}

