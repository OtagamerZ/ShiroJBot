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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.helpers.CollectionHelper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class KawaiponDAO {
	public static Kawaipon getKawaipon(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return CollectionHelper.getOr(em.find(Kawaipon.class, id), new Kawaipon(id));
		} finally {
			em.close();
		}
	}

	public static void saveKawaipon(Kawaipon k) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(k);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeKawaipon(Kawaipon k) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(em.contains(k) ? k : em.merge(k));
		em.getTransaction().commit();

		em.close();
	}

	public static Deck getDeck(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT d
				FROM Kawaipon kp
				JOIN kp.decks d
				WHERE kp.uid = :uid
				AND d.id = kp.activeDeck
				""", Deck.class);
		q.setParameter("uid", id);

		try {
			return (Deck) q.getSingleResult();
		} catch (NoResultException e) {
			return getKawaipon(id).getDeck();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Deck> getDecks(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT d
				FROM Kawaipon kp
				JOIN kp.decks d
				WHERE kp.uid = :uid
				""", Deck.class);
		q.setParameter("uid", id);

		try {
			return (List<Deck>) q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Deck> getDecks() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT d
				FROM Kawaipon kp
				JOIN kp.decks d
				""", Deck.class);

		try {
			return (List<Deck>) q.getResultList();
		} finally {
			em.close();
		}
	}

	public static void saveDeck(Deck d) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(d);
		em.getTransaction().commit();

		em.close();
	}

	public static Hero getHero(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT h
				FROM Kawaipon kp
				JOIN kp.heroes h
				WHERE kp.uid = :uid
				AND h.id = kp.activeHero
				""", Hero.class);
		q.setParameter("uid", id);

		try {
			return (Hero) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Hero> getHeroes(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT h
				FROM Kawaipon kp
				JOIN kp.heroes h
				WHERE kp.uid = :uid
				""", Hero.class);
		q.setParameter("uid", id);

		try {
			return (List<Hero>) q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Hero> getHeroes() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT h
				FROM Kawaipon kp
				JOIN kp.heroes h
				""", Hero.class);

		try {
			return (List<Hero>) q.getResultList();
		} finally {
			em.close();
		}
	}

	public static void saveHero(Hero h) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(h);
		em.getTransaction().commit();

		em.close();
	}
}
