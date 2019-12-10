package com.kuuhaku.controller.MySQL;

import com.kuuhaku.model.PermaBlock;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class RelayDAO {
	public static void permaBlock(PermaBlock p) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(p);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<String> blockedList() {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createQuery("SELECT p.id FROM PermaBlock p", String.class);
			List<String> blocks = q.getResultList();
			em.close();
			return blocks;
		} catch (NoResultException e) {
			em.close();
			return new ArrayList<>();
		}
	}
}
