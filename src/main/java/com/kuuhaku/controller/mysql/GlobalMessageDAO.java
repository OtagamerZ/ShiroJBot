package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.GlobalMessage;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class GlobalMessageDAO {
	@SuppressWarnings("unchecked")
	public static List<GlobalMessage> getMessages() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM GlobalMessage m", GlobalMessage.class);

		try {
			return (List<GlobalMessage>) q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static void saveMessage(GlobalMessage gm) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(gm);
		em.getTransaction().commit();

		em.close();
	}
}
