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

import com.kuuhaku.model.persistent.FieldMarket;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class FieldMarketDAO {
	@SuppressWarnings("unchecked")
	public static List<FieldMarket> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT fm FROM FieldMarket fm WHERE buyer = ''", FieldMarket.class);

		try {
			return (List<FieldMarket>) q.getResultList();
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
			return (List<FieldMarket>) q.getResultList();
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
}
