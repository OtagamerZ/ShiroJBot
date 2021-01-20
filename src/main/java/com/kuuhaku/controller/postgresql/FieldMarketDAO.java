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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
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

		Query q = em.createQuery("SELECT fm FROM EquipmentMarket fm WHERE fm.card.card.id = UPPER(:id) AND fm.publishDate IS NOT NULL", FieldMarket.class);
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

	public static double getAverageValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT AVG(fm.price)
				FROM FieldMarket fm
				WHERE fm.card.card = :card
				AND fm.publishDate >= :date
				""");
		q.setParameter("card", c);
		q.setParameter("date", OffsetDateTime.now().minusMonths(1));

		try {
			return ((BigDecimal) q.getSingleResult()).doubleValue();
		} finally {
			em.close();
		}
	}

	public static double getStockValue(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT AVG(fm.price)
				FROM FieldMarket fm
				WHERE fm.card.card = :card
				AND fm.publishDate >= :date
				""");
		q.setParameter("card", c);
		q.setParameter("date", OffsetDateTime.now().minusMonths(1));

		double before = ((BigDecimal) q.getSingleResult()).setScale(3, RoundingMode.HALF_EVEN).doubleValue();

		q = em.createQuery("""
				SELECT AVG(fm.price)
				FROM FieldMarket fm
				WHERE fm.card.card = :card
				AND fm.publishDate < :date
				""");
		q.setParameter("card", c);
		q.setParameter("date", OffsetDateTime.now().minusMonths(1));

		double now = ((BigDecimal) q.getSingleResult()).setScale(3, RoundingMode.HALF_EVEN).doubleValue();

		try {
			return Helper.prcnt(now, before) - 1;
		} finally {
			em.close();
		}
	}
}
