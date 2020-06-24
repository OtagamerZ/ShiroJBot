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
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.AnimeName;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class CardDAO {
	public static KawaiponCard getCard(Kawaipon kp, String name, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE name LIKE UPPER(:name)", Card.class);
		q.setParameter("name", name);

		try {
			if (kp.getCards().stream().anyMatch(k -> k.getCard().getId().equalsIgnoreCase(name) && k.isFoil() == foil))
				return new KawaiponCard(null, (Card) q.getSingleResult(), foil);
			else return null;
		} catch (NoResultException e) {
			System.out.println("Not found");
			return null;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Card> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c", Card.class);
		List<Card> c = (List<Card>) q.getResultList();

		em.close();

		return c;
	}

	public static long totalCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT COUNT(c) FROM Card c", Long.class);

		try {
			return ((long) q.getSingleResult()) * 2;
		} finally {
			em.close();
		}
	}

	public static long animeCount(AnimeName anime) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT COUNT(c) FROM Card c WHERE anime = :anime", Long.class);
		q.setParameter("anime", anime);

		try {
			return ((long) q.getSingleResult()) * 2;
		} finally {
			em.close();
		}
	}
}
