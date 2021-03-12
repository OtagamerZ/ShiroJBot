/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.controller.sqlite;

import com.kuuhaku.model.persistent.Member;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class MemberDAO {
	public static Member getMember(String id, String server) {
		EntityManager em = Manager.getEntityManager();

		try {
			Member mb = em.find(Member.class, id + server);
			if (mb == null)
				return addMemberToDB(id, server);

			return mb;
		} finally {
			em.close();
		}
	}

	public static Member getHighestProfile(String id) {
		EntityManager em = Manager.getEntityManager();
		Member m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE uid = :id ORDER BY level DESC", Member.class);
		q.setMaxResults(1);
		q.setParameter("id", id);
		m = (Member) q.getSingleResult();

		em.close();

		return m;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberByMid(String id) {
		EntityManager em = Manager.getEntityManager();
		List<Member> m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE uid = :id", Member.class);
		q.setParameter("id", id);
		m = q.getResultList();

		em.close();

		return m;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberBySid(String id) {
		EntityManager em = Manager.getEntityManager();
		List<Member> m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE sid = :id", Member.class);
		q.setParameter("id", id);
		m = q.getResultList();

		em.close();

		return m;
	}

	public static int getHighestLevel() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT MAX(level) FROM Member m", Integer.class);

		try {
			return (int) q.getSingleResult();
		} finally {
			em.close();
		}
	}

	public static Member addMemberToDB(String id, String server) {
		EntityManager em = Manager.getEntityManager();

		Member m = new Member();
		m.setId(id + server);
		m.setUid(id);
		m.setSid(server);

		em.getTransaction().begin();
		m = em.merge(m);
		em.getTransaction().commit();

		em.close();

		return m;
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
			q = em.createQuery("SELECT m FROM Member m WHERE m.uid IS NOT NULL ORDER BY m.level DESC, m.xp DESC", Member.class);
		else {
			q = em.createQuery("SELECT m FROM Member m WHERE m.sid = :id AND m.uid IS NOT NULL ORDER BY m.level DESC, m.xp DESC", Member.class);
			q.setParameter("id", gid);
		}

		List<Member> mbs = q.getResultList();

		em.close();

		return mbs;
	}

	public static int getMemberRankPos(String mid, String gid, boolean global) {
		EntityManager em = Manager.getEntityManager();

		Query q;

		if (global)
			q = em.createNativeQuery("""
					SELECT x.row
					FROM (
						SELECT m.uid
							 , row_number() OVER (ORDER BY m.level DESC, m.xp DESC) AS row 
						FROM Member m 
						WHERE m.uid IS NOT NULL
					) x
					WHERE x.uid = :mid
					""");
		else {
			q = em.createNativeQuery("""
					SELECT x.row
					FROM (
						SELECT m.uid
							 , row_number() OVER (ORDER BY m.level DESC, m.xp DESC) AS row 
						FROM Member m 
						WHERE m.sid = :id 
						AND m.uid IS NOT NULL
					) x
					WHERE x.uid = :mid
					""");
			q.setParameter("id", gid);
		}
		q.setParameter("mid", mid);
		q.setMaxResults(1);

		try {
			return ((Number) q.getSingleResult()).intValue();
		} catch (NoResultException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberVoiceRank(String gid) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m WHERE m.sid = :id AND m.uid IS NOT NULL ORDER BY m.voiceTime DESC", Member.class);
		q.setParameter("id", gid);

		List<Member> mbs = q.getResultList();

		em.close();

		return mbs;
	}

	public static int getMemberVoiceRankPos(String mid, String gid) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT x.row
				FROM (
					SELECT m.uid
						 , row_number() OVER (ORDER BY m.voiceTime DESC) AS row 
					FROM Member m 
					WHERE m.sid = :id 
					AND m.uid IS NOT NULL
				) x
				WHERE x.uid = :mid
				""");
		q.setParameter("id", gid);
		q.setParameter("mid", mid);
		q.setMaxResults(1);

		try {
			return ((Number) q.getSingleResult()).intValue();
		} catch (NoResultException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getAllMembers() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT m FROM Member m", Member.class);
		List<Member> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	public static void clearMember(String id) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Query q = em.createQuery("DELETE FROM Member m WHERE m.uid = :id");
		q.setParameter("id", id);
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}
}
