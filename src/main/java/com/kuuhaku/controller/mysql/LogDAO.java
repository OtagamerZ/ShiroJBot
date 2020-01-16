package com.kuuhaku.controller.mysql;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class LogDAO {
	public static void saveLog(com.kuuhaku.model.Log log) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(log);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Object[]> getUses() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT guild, COUNT(guild) AS uses FROM Log l GROUP BY guild ORDER BY uses DESC");

		return q.getResultList();
	}
}
