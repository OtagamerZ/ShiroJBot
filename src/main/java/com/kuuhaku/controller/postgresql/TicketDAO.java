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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.model.persistent.Ticket;

import javax.persistence.EntityManager;
import java.util.Map;

public class TicketDAO {
	public static Ticket getTicket(int id) {
		EntityManager em = Manager.getEntityManager();

		Ticket t = em.find(Ticket.class, id);

		em.close();

		return t;
	}

	public static void updateTicket(Ticket t) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}

	public static int getNumber() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Ticket t = new Ticket();
		em.merge(t);
		em.getTransaction().commit();

		em.flush();

		try {
			return t.getNumber();
		} finally {
			em.close();
		}
	}

	public static void setIds(int id, Map<String, String> msgs) {
		EntityManager em = Manager.getEntityManager();

		Ticket t = em.find(Ticket.class, id);
		t.setMsgIds(msgs);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();
	}
}
