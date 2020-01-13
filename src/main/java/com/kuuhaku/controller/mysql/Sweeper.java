package com.kuuhaku.controller.mysql;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class Sweeper {
	public static void sweep() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("DELETE FROM GuildConfig WHERE markForDelete = TRUE");
		q.executeUpdate();

		q = em.createQuery("DELETE FROM Member WHERE markForDelete = TRUE");
		q.executeUpdate();

		em.close();
	}
}
