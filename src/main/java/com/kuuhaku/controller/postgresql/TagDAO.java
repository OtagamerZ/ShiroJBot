/*
 * This file is part of Shiro J Bot.
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
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class TagDAO {
	@SuppressWarnings("unchecked")
	public static List<Tags> getAllPartners() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tags t WHERE t.Partner = true", Tags.class);
		List<Tags> ts = (List<Tags>) q.getResultList();

		em.close();

		return ts;
	}

	@SuppressWarnings("unchecked")
	public static List<Tags> getAllTags() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tags t", Tags.class);
		List<Tags> ts = (List<Tags>) q.getResultList();

		em.close();

		return ts;
	}

	public static Tags getTagById(String id) {
		EntityManager em = Manager.getEntityManager();
		Tags m;

		try {
			Query q = em.createQuery("SELECT t FROM Tags t WHERE t.id = ?1", Tags.class);
			q.setParameter(1, id);
			m = (Tags) q.getSingleResult();

			em.close();

			return m;
		} catch (NoResultException e) {
			TagDAO.addUserTagsToDB(id);
			Tags t = new Tags();
			t.setId(id);
			return t;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Tags> getSponsors() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Tags t WHERE t.Sponsor = true", Tags.class);
		List<Tags> ts = (List<Tags>) q.getResultList();

		em.close();

		return ts;
	}

	public static int getPartnerAmount() {
		EntityManager em = Manager.getEntityManager();
		int size;

		Query q = em.createQuery("SELECT t FROM Tags t WHERE t.Partner = true", Tags.class);
		size = q.getResultList().size();

		em.close();

		return size;
	}

	public static void addUserTagsToDB(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = new Tags();
		t.setId(id);

		em.getTransaction().begin();
		em.merge(t);
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

	public static void giveTagPartner(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setPartner(true);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeTagPartner(String id) {
		EntityManager em = Manager.getEntityManager();

		Tags t = getTagById(id);
		t.setPartner(false);

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
