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
import javax.persistence.Query;
import java.util.List;

public class MemberDAO {
	public static Member getMemberById(String id) {
		EntityManager em = Manager.getEntityManager();
		Member m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE id = :id", Member.class);
		q.setParameter("id", id);
		m = (Member) q.getSingleResult();

		em.close();

		return m;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberByMid(String id) {
		EntityManager em = Manager.getEntityManager();
		List<Member> m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE mid = :id", Member.class);
		q.setParameter("id", id);
		m = (List<Member>) q.getResultList();

		em.close();

		return m;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberBySid(String id) {
		EntityManager em = Manager.getEntityManager();
		List<Member> m;

		Query q = em.createQuery("SELECT m FROM Member m WHERE sid = :id", Member.class);
		q.setParameter("id", id);
		m = (List<Member>) q.getResultList();

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

	public static void addMemberToDB(net.dv8tion.jda.api.entities.Member u) {
		if (u == null) return;
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
			q = em.createQuery("SELECT m FROM Member m WHERE id LIKE :id AND m.mid IS NOT NULL ORDER BY m.level DESC", Member.class);
			q.setParameter("id", "%" + gid);
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
}
