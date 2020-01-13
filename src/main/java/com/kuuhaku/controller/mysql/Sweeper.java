package com.kuuhaku.controller.mysql;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class Sweeper {
	public static int sweep() {
		EntityManager em = Manager.getEntityManager();

		int affected = 0;

		Query q = em.createQuery("DELETE FROM GuildConfig WHERE markForDelete = TRUE");
		affected += q.executeUpdate();

		q = em.createQuery("DELETE FROM Member WHERE markForDelete = TRUE");
		affected += q.executeUpdate();

		em.close();

		return affected;
	}
}
