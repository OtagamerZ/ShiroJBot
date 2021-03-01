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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.model.persistent.MutedMember;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class MemberDAO {
	@SuppressWarnings("unchecked")
	public static List<Member> getMembers() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m", Member.class);
		List<Member> members = q.getResultList();
		em.close();

		return members;
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

	public static void saveMemberToBD(Member m) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
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
		EntityManager em = com.kuuhaku.controller.sqlite.Manager.getEntityManager();

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

		com.kuuhaku.controller.sqlite.MemberDAO.clearMember(id);
	}
}
