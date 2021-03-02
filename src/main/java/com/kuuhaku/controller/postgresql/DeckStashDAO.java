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

import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.DeckStash;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class DeckStashDAO {
	@SuppressWarnings("unchecked")
	public static List<DeckStash> getStash(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT ds FROM DeckStash ds WHERE ds.uid = :id", DeckStash.class);
		q.setParameter("id", id);

		try {
			Account acc = AccountDAO.getAccount(id);
			List<DeckStash> stashes = q.getResultList();

			if (stashes.size() < acc.getStashCapacity()) {
				for (int i = stashes.size(); i < acc.getStashCapacity(); i++)
					saveStash(new DeckStash(id));

				stashes = getStash(id);
			}

			return stashes;
		} finally {
			em.close();
		}
	}

	public static void saveStash(DeckStash ds) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(ds);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeKawaipon(DeckStash ds) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(ds);
		em.getTransaction().commit();

		em.close();
	}
}
