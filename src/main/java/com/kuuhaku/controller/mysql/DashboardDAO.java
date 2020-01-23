package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.AppUser;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class DashboardDAO {
	public static AppUser getData(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT u FROM AppUser u WHERE uid LIKE :id", AppUser.class);
		q.setParameter("id", id);

		try {
			return (AppUser) q.getSingleResult();
		} finally {
			em.close();
		}
	}

	public static void saveData(AppUser u) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(u);
		em.getTransaction().commit();

		em.close();
	}
}
