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

import com.kuuhaku.model.persistent.Lottery;
import com.kuuhaku.model.persistent.LotteryValue;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class LotteryDAO {
	public static LotteryValue getLotteryValue() {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(LotteryValue.class, 0);
		} finally {
			em.close();
		}
	}

	public static void saveLotteryValue(LotteryValue lv) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(lv);
		em.getTransaction().commit();

		em.close();
	}

	public static void saveLottery(Lottery ltt) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(ltt);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Lottery> getLotteries() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT l FROM Lottery l WHERE l.valid = TRUE", Lottery.class);

		List<Lottery> ltts = q.getResultList();

		em.close();

		return ltts;
	}

	@SuppressWarnings("unchecked")
	public static List<Lottery> getLotteriesByUser(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT l FROM Lottery l WHERE l.uid = :id AND l.valid = TRUE", Lottery.class);
		q.setParameter("id", id);

		List<Lottery> ltts = q.getResultList();

		em.close();

		return ltts;
	}

	@SuppressWarnings("unchecked")
	public static List<Lottery> getLotteriesByDozens(String dozens) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT l FROM Lottery l WHERE l.dozens = :dozens AND l.valid = TRUE", Lottery.class);
		q.setParameter("dozens", dozens);

		List<Lottery> ltts = q.getResultList();

		em.close();

		return ltts;
	}

	public static void closeLotteries() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("UPDATE Lottery l SET l.valid = FALSE");

		em.getTransaction().begin();
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
