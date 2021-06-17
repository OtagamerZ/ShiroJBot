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

import com.kuuhaku.model.persistent.Ticket;
import net.dv8tion.jda.api.entities.Member;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class TicketDAO {
	public static Ticket getTicket(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Ticket.class, id);
		} finally {
			em.close();
		}
	}

	public static void updateTicket(Ticket t) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static int openTicket(String subject, Member m) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Ticket t = new Ticket(subject, m);
		em.merge(t);
		em.getTransaction().commit();

		try {
			return getNumber();
		} finally {
			em.close();
		}
	}

	private static int getNumber() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t.number FROM Ticket t ORDER BY id DESC");
		q.setMaxResults(1);

		try {
			return (int) q.getSingleResult();
		} finally {
			em.close();
		}
	}
}
