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

import com.kuuhaku.model.persistent.Trade;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class TradeDAO {
	@SuppressWarnings("unchecked")
	public static Trade getTrade(String uid, String target) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT t
				FROM Trade t
				JOIN t.offers o
				WHERE t.finished = FALSE
				AND o.uid = :uid
				""", Trade.class);
		q.setParameter("uid", uid);
		q.setMaxResults(1);

		try {
			List<Trade> trades = (List<Trade>) q.getResultList();
			trades.removeIf(t -> !t.getOffers().stream().allMatch(to -> Helper.equalsAny(to.getUid(), uid, target)));

			return Helper.safeGet(trades, 0);
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static void saveTrade(Trade t) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeTrade(Trade t) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(em.contains(t) ? t : em.merge(t));
		em.getTransaction().commit();

		em.close();
	}
}
