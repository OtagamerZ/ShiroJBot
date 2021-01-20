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

import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.StockMarket;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class StockMarketDAO {
	@SuppressWarnings("unchecked")
	public static List<StockMarket> getInvestments(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT sm FROM StockMarket sm WHERE sm.uid = :id", StockMarket.class);
		q.setParameter("id", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static StockMarket getCardInvestment(String id, Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT sm FROM StockMarket sm WHERE sm.uid = :id AND sm.card = :card", StockMarket.class);
		q.setParameter("id", id);
		q.setParameter("card", c);

		try {
			return (StockMarket) q.getSingleResult();
		} catch (NoResultException e) {
			return new StockMarket(id, c, 0);
		} finally {
			em.close();
		}
	}

	public static void saveInvestment(StockMarket sm) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(sm);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeZeroInvestments() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("DELETE FROM StockMarket sm WHERE sm.investment = 0")
				.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
