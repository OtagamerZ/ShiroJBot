/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.persistent.Starboard;
import net.dv8tion.jda.api.entities.Message;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class StarboardDAO {
	public static void starboard(Message msg) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(new Starboard(msg));
		em.getTransaction().commit();

		em.close();
	}

	public static boolean isStarboarded(Message msg) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT s FROM Starboard s WHERE guild = :guild AND message = :msg AND author = :author", Starboard.class);
		q.setParameter("guild", msg.getGuild().getId());
		q.setParameter("msg", msg.getId());
		q.setParameter("author", msg.getAuthor().getId());

		try {
			q.getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} finally {
			em.close();
		}
	}
}
