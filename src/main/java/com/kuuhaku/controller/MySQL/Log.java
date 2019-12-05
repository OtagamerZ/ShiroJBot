package com.kuuhaku.controller.MySQL;

import javax.persistence.EntityManager;

public class Log {
	public static void saveLog(com.kuuhaku.model.Log log) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(log);
		em.getTransaction().commit();

		em.close();
	}
}
