package com.kuuhaku.controller.MySQL;

import com.kuuhaku.model.Tags;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class TagDAO {
	public static Tags getTagById(String id) {
		EntityManager em = Manager.getEntityManager();
		Tags m;

		Query q = em.createQuery("SELECT t FROM Tags t WHERE id = ?1", Tags.class);
		q.setParameter(1, id);
		m = (Tags) q.getSingleResult();

		em.close();

		return m;
	}

	public static int getPartnerAmount() {
		EntityManager em = Manager.getEntityManager();
		int size;

		Query q = em.createQuery("SELECT t FROM Tags t WHERE Partner = true", Tags.class);
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
