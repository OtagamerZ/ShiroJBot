/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.model.persistent.id.CompositeMemberId;
import com.kuuhaku.utils.helpers.CollectionHelper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.List;

public class MemberDAO {
	public static long getMemberCount() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT COUNT(1) FROM Member");

		try {
			return ((BigInteger) q.getSingleResult()).longValue();
		} finally {
			em.close();
		}
	}

	public static Member getMember(String id, String guild) {
		EntityManager em = Manager.getEntityManager();

		try {
			return CollectionHelper.getOr(em.find(Member.class, new CompositeMemberId(id, guild)), new Member(id, guild));
		} finally {
			em.close();
		}
	}

	public static void saveMember(Member m) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMembers() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m", Member.class);
		List<Member> members = q.getResultList();
		em.close();

		return members;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMembersByUid(String id) {
		EntityManager em = Manager.getEntityManager();
		List<Member> m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE uid = :id", Member.class);
		q.setParameter("id", id);
		m = q.getResultList();

		em.close();

		return m;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMembersBySid(String id) {
		EntityManager em = Manager.getEntityManager();
		List<Member> m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE sid = :id", Member.class);
		q.setParameter("id", id);
		m = q.getResultList();

		em.close();

		return m;
	}

	public static Member getHighestProfile(String id) {
		EntityManager em = Manager.getEntityManager();
		Member m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE uid = :id ORDER BY xp DESC", Member.class);
		q.setMaxResults(1);
		q.setParameter("id", id);
		m = (Member) q.getSingleResult();

		em.close();

		return m;
	}

	public static int getHighestLevel() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT MAX(xp) FROM Member m");

		try {
			return (int) Math.sqrt((long) q.getSingleResult() / 100d);
		} finally {
			em.close();
		}
	}

	public static int getMemberRankPos(String mid, String gid, boolean global) {
		EntityManager em = Manager.getEntityManager();

		Query q;

		if (global)
			q = em.createNativeQuery("""
					SELECT x.row
					FROM (
							 SELECT m.uid
								  , row_number() OVER (ORDER BY m.xp DESC) AS row
							 FROM member m
							 INNER JOIN guildconfig gc ON gc.guildid = m.sid
							 WHERE NOT EXISTS(SELECT b.uid FROM blacklist b WHERE b.uid = m.uid)
						 ) x
					WHERE x.uid = :mid
					""");
		else {
			q = em.createNativeQuery("""
					SELECT x.row
					FROM (
							 SELECT m.uid
								  , row_number() OVER (ORDER BY m.xp DESC) AS row
							 FROM member m
							 INNER JOIN guildconfig gc ON gc.guildid = m.sid
							 WHERE NOT EXISTS(SELECT b.uid FROM blacklist b WHERE b.uid = m.uid)
							   AND m.sid = :guild
						 ) x
					WHERE x.uid = :mid
					""");
			q.setParameter("guild", gid);
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
	public static List<MutedMember> getMutedMembers() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM MutedMember m", MutedMember.class);
		List<MutedMember> members = q.getResultList();
		em.close();

		return members;
	}

	public static MutedMember getMutedMemberById(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(MutedMember.class, id);
		} finally {
			em.close();
		}
	}

	public static void saveMutedMember(MutedMember m) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeMutedMember(MutedMember m) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Query q = em.createQuery("DELETE FROM MutedMember m WHERE m.id = :id");
		q.setParameter("id", m.getUid());
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getAllMembers() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT m FROM Member m", Member.class);
		List<Member> gcs = gc.getResultList();

		em.close();

		return gcs;
	}
}
