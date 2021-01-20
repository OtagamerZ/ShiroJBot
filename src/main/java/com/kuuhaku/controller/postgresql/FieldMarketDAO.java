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
import com.kuuhaku.model.persistent.FieldMarket;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Calendar;
import java.util.List;

public class FieldMarketDAO {
	@SuppressWarnings("unchecked")
	public static List<FieldMarket> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT fm FROM FieldMarket fm WHERE buyer = ''", FieldMarket.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<FieldMarket> getCardsByCard(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT fm FROM FieldMarket fm WHERE fm.card.card.id = UPPER(:id) AND fm.publishDate IS NOT NULL", FieldMarket.class);
		q.setParameter("id", id);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	public static FieldMarket getCard(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			FieldMarket fm = em.find(FieldMarket.class, id);
			if (fm == null || !fm.getBuyer().isBlank()) return null;
			else return fm;
		} finally {
			em.close();
		}
	}

	public static void saveCard(FieldMarket card) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(card);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static double getAverageValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);

		Query q = em.createQuery("""
				SELECT fm.price
				FROM FieldMarket fm
				WHERE fm.card.card = :card
				AND fm.publishDate >= :date
				AND fm.buyer <> ''
				AND fm.buyer <> fm.seller
				AND fm.price / :average BETWEEN -0.75 AND 0.75 
				""");
		q.setParameter("card", c);
		q.setParameter("date", cal.getTime());

		List<Integer> values = (List<Integer>) q.getResultList();

		double avg = values.stream()
				.mapToInt(i -> i)
				.average()
				.orElse(0);

		try {
			return values.stream().filter(i -> i / (avg == 0 ? i / 2d : avg) <= 0.75).mapToInt(i -> i).average().orElse(0);
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static double getStockValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		try {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -1);

			Query q = em.createQuery("""
					SELECT fm.price
					FROM FieldMarket fm
					WHERE fm.card.card = :card
					AND fm.publishDate >= :date
					AND fm.buyer <> ''
					AND fm.buyer <> fm.seller
					AND fm.price / :average BETWEEN -0.75 AND 0.75 
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());

			List<Integer> before = (List<Integer>) q.getResultList();
			double avgb = before.stream()
					.mapToInt(i -> i)
					.average()
					.orElse(0);

			q = em.createQuery("""
					SELECT fm.price
					FROM FieldMarket fm
					WHERE fm.card.card = :card
					AND fm.publishDate < :date
					AND fm.buyer <> ''
					AND fm.buyer <> fm.seller
					AND fm.price / :average BETWEEN -0.75 AND 0.75 
					""");
			q.setParameter("card", c);
			q.setParameter("date", cal.getTime());

			List<Integer> now = (List<Integer>) q.getResultList();
			double avgn = now.stream()
					.mapToInt(i -> i)
					.average()
					.orElse(0);

			double aBefore = before.stream().filter(i -> i / (avgb == 0 ? i / 2d : avgb) <= 0.75).mapToInt(i -> i).average().orElse(0);
			double aNow = now.stream().filter(i -> i / (avgn == 0 ? i / 2d : avgn) <= 0.75).mapToInt(i -> i).average().orElse(0);

			return Helper.prcnt(aNow, aBefore) - 1;
		} catch (NullPointerException e) {
			return 0;
		} finally {
			em.close();
		}
	}
}
