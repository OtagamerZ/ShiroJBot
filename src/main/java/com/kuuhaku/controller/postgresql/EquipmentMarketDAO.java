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
import com.kuuhaku.model.persistent.EquipmentMarket;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Calendar;
import java.util.List;

public class EquipmentMarketDAO {
	@SuppressWarnings("unchecked")
	public static List<EquipmentMarket> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT eqm FROM EquipmentMarket eqm WHERE buyer = ''", EquipmentMarket.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<EquipmentMarket> getCardsByCard(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT eqm FROM EquipmentMarket eqm WHERE eqm.card.card.id = UPPER(:id) AND eqm.publishDate IS NOT NULL", EquipmentMarket.class);
		q.setParameter("id", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static EquipmentMarket getCard(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			EquipmentMarket eqm = em.find(EquipmentMarket.class, id);
			if (eqm == null || !eqm.getBuyer().isBlank()) return null;
			else return eqm;
		} finally {
			em.close();
		}
	}

	public static void saveCard(EquipmentMarket card) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(card);
		em.getTransaction().commit();

		em.close();
	}

	public static double getAverageValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);

		double average;
		try {
			average = (double) em.createQuery("""
					SELECT AVG(em.price)
					FROM EquipmentMarket em
					WHERE em.card.card = :card
					AND em.buyer <> ''
					AND em.buyer <> em.seller
					""").setParameter("card", c)
					.getSingleResult();
		} catch (NullPointerException e) {
			average = 0;
		}

		Query q = em.createQuery("""
				SELECT AVG(em.price)
				FROM EquipmentMarket em
				WHERE em.card.card = :card
				AND em.publishDate >= :date
				AND em.buyer <> ''
				AND em.buyer <> em.seller
				AND em.price / :average BETWEEN -0.5 AND 0.5 
				""");
		q.setParameter("card", c);
		q.setParameter("date", cal.getTime());
		q.setParameter("average", average);

		try {
			return (Double) q.getSingleResult();
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	public static double getStockValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		double average;
		try {
			average = (double) em.createQuery("""
					SELECT AVG(em.price)
					FROM EquipmentMarket em
					WHERE em.card.card = :card
					AND em.buyer <> ''
					AND em.buyer <> em.seller
					""").setParameter("card", c)
					.getSingleResult();
		} catch (NullPointerException e) {
			average = 0;
		}

		try {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -1);

			Query q = em.createQuery("""
					SELECT AVG(em.price)
					FROM EquipmentMarket em
					WHERE em.card.card = :card
					AND em.publishDate >= :date
					AND em.buyer <> ''
					AND em.buyer <> em.seller
					AND em.price / :average BETWEEN -0.5 AND 0.5 
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());
			q.setParameter("average", average);

			double before = Helper.round((Double) q.getSingleResult(), 3);

			q = em.createQuery("""
					SELECT AVG(em.price)
					FROM EquipmentMarket em
					WHERE em.card.card = :card
					AND em.publishDate < :date
					AND em.buyer <> ''
					AND em.buyer <> em.seller
					AND em.price / :average BETWEEN -0.5 AND 0.5 
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());
			q.setParameter("average", average);

			double now = Helper.round((Double) q.getSingleResult(), 3);

			return Helper.prcnt(now, before) - 1;
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}
}
