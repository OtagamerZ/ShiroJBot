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

import com.kuuhaku.model.persistent.Tags;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class TagDAO {
	@SuppressWarnings("unchecked")
	public static List<Tags> getAllBetas() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tags t WHERE t.beta = true", Tags.class);
		List<Tags> ts = q.getResultList();

		em.close();

		return ts;
	}

	@SuppressWarnings("unchecked")
	public static List<Tags> getAllTags() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tags t", Tags.class);
		List<Tags> ts = q.getResultList();

		em.close();

		return ts;
	}

	public static Tags getTagById(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Tags t = em.find(Tags.class, id);
			if (t == null) {
				addUserTagsToDB(id);
				return getTagById(id);
			} else return t;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Tags> getSponsors() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tags t WHERE t.sponsor = true", Tags.class);
		List<Tags> ts = q.getResultList();

		em.close();

		return ts;
	}

	public static void addUserTagsToDB(String id) {
		if (BlacklistDAO.isBlacklisted(id)) return;
		EntityManager em = Manager.getEntityManager();

		Tags t = em.find(Tags.class, id);
		if (t == null) {
			t = new Tags(id);

			em.getTransaction().begin();
			em.merge(t);
			em.getTransaction().commit();

			em.close();
		}
	}

	public static void clearTags(Tags t) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Query q = em.createQuery("DELETE FROM Tags t WHERE t.uid = :id");
		q.setParameter("id", t.getUid());
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	public static void giveTagToxic(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setToxic(true);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeTagToxic(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setToxic(false);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void giveTagBeta(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setBeta(true);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeTagBeta(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setBeta(false);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void giveTagVerified(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setVerified(true);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeTagVerified(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setVerified(false);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void giveTagReader(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setReader(true);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}
}
