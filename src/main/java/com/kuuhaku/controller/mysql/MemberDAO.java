/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.controller.mysql;

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
