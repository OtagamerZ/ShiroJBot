package com.kuuhaku.controller.MySQL;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class MemberDAO {
	@SuppressWarnings("unchecked")
	public static List<com.kuuhaku.model.Member> getMembers() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m", com.kuuhaku.model.Member.class);
		List<com.kuuhaku.model.Member> members = q.getResultList();
		em.close();

		return members;
	}

	public static void saveMemberToBD(com.kuuhaku.model.Member m) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}
}
