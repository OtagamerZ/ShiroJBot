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
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class WaifuDAO {
	public static void saveMemberWaifu(Member m, User u) {
		EntityManager em = Manager.getEntityManager();

		m.marry(u);

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeMemberWaifu(Member m) {
		EntityManager em = Manager.getEntityManager();

		m.divorce();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static boolean isWaifued(String id) {
		EntityManager em = Manager.getEntityManager();

		boolean married = false;

		Query q = em.createQuery("SELECT m FROM Member m WHERE mid LIKE :id AND waifu IS NOT NULL AND waifu NOT LIKE ''");
		q.setParameter("id", id);

		married = q.getResultList().size() > 0;

		em.close();

		return married;
	}
}
