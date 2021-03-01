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

import com.kuuhaku.model.persistent.Couple;
import com.kuuhaku.model.persistent.CoupleMultiplier;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class WaifuDAO {
	public static void saveCouple(User h, User w) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(new Couple(h.getId(), w.getId()));
		if (em.find(CoupleMultiplier.class, h.getId()) == null) em.merge(new CoupleMultiplier(h.getId()));
		if (em.find(CoupleMultiplier.class, w.getId()) == null) em.merge(new CoupleMultiplier(w.getId()));
		em.getTransaction().commit();

		em.close();
	}

	public static void removeCouple(User u) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Couple c WHERE husbando = :id OR waifu = :id");
		q.setParameter("id", u.getId());

		CoupleMultiplier cm = getMultiplier(u);

		em.getTransaction().begin();
		em.remove(q.getSingleResult());
		if (cm != null) {
			cm.decrease();
			em.merge(cm);
		}
		em.getTransaction().commit();

		em.close();
	}

	public static void voidCouple(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Couple c WHERE husbando = :id OR waifu = :id");
		q.setParameter("id", id);

		em.getTransaction().begin();
		em.remove(q.getSingleResult());
		removeMultiplier(id);
		em.getTransaction().commit();

		em.close();
	}

	public static Couple getCouple(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Couple c WHERE husbando = :id OR waifu = :id");
		q.setParameter("id", id);
		q.setMaxResults(1);

		try {
			return (Couple) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static boolean isWaifued(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Couple c WHERE husbando = :id OR waifu = :id");
		q.setParameter("id", id);

		try {
			q.getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} finally {
			em.close();
		}
	}

	public static CoupleMultiplier getMultiplier(User u) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(CoupleMultiplier.class, u.getId());
		} finally {
			em.close();
		}
	}

	public static void removeMultiplier(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("DELETE FROM CoupleMultiplier c WHERE c.uid = :id");
		q.setParameter("id", id);

		em.getTransaction().begin();
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
