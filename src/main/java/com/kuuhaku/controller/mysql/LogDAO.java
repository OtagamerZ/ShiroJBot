package com.kuuhaku.controller.mysql;

import javax.persistence.EntityManager;

public class LogDAO {
	public static void saveLog(com.kuuhaku.model.Log log) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(log);
		em.getTransaction().commit();

		em.close();
	}
}
