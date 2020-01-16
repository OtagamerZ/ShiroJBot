package com.kuuhaku.controller.sqlite;

import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.Member;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class MemberDAO {
	public static Member getMemberById(String id) {
		EntityManager em = Manager.getEntityManager();
		Member m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE id LIKE ?1", Member.class);
		q.setParameter(1, id);
		m = (Member) q.getSingleResult();

		em.close();

		return m;
	}

	public static Member getMemberByMid(String id) {
		EntityManager em = Manager.getEntityManager();
		Member m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE mid LIKE ?1", Member.class);
		q.setParameter(1, id);
		q.setMaxResults(1);
		m = (Member) q.getSingleResult();

		em.close();

		return m;
	}

	public static void addMemberToDB(net.dv8tion.jda.api.entities.Member u) {
		EntityManager em = Manager.getEntityManager();

		Member m = new Member();
		m.setId(u.getUser().getId() + u.getGuild().getId());
		m.setMid(u.getUser().getId());
		m.setSid(u.getGuild().getId());

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void updateMemberConfigs(Member m) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberRank(String gid, boolean global) {
		EntityManager em = Manager.getEntityManager();

		Query q;

		if (global)
			q = em.createQuery("SELECT m FROM Member m WHERE m.mid IS NOT NULL ORDER BY m.level DESC", Member.class);
		else {
			q = em.createQuery("SELECT m FROM Member m WHERE id LIKE ?1 AND m.mid IS NOT NULL ORDER BY m.level DESC", Member.class);
			q.setParameter(1, "%" + gid);
		}

		List<Member> mbs = (List<Member>) q.getResultList();

		em.close();

		return mbs;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getAllMembers() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT m FROM Member m", Member.class);
		List<Member> gcs = (List<Member>) gc.getResultList();

		em.close();

		return gcs;
	}

	public static Member authMember(String login, String password) {
		EntityManager em = com.kuuhaku.controller.mysql.Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m WHERE login LIKE :login AND password LIKE :pass", Member.class);
		q.setParameter("login", login);
		q.setParameter("pass", password);

		try {
			return (Member) q.getSingleResult();
		} catch (NoResultException e) {
			throw new UnauthorizedException();
		} finally {
			em.close();
		}
	}
}
