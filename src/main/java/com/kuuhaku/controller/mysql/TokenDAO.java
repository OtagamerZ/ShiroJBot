package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.Token;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class TokenDAO {
	public static boolean validateToken(String token) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE token LIKE :token", Token.class);
		q.setParameter("token", token);
		q.setMaxResults(1);

		try {
			Token t = (Token) q.getSingleResult();

			em.getTransaction().begin();
			em.merge(t.addCall());
			em.getTransaction().commit();

			em.close();

			return true;
		} catch (NoResultException e) {
			return false;
		}
	}
}
