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

import com.kuuhaku.model.persistent.Token;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class TokenDAO {
	public static boolean validateToken(String token) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE token LIKE :token", Token.class);
		q.setParameter("token", token);
		q.setMaxResults(1);

		try {
			Token t = (Token) q.getSingleResult();

			em.getTransaction().begin();
			em.merge(t.addCall());
			em.getTransaction().commit();

			em.close();

			return true;
		} catch (NoResultException e) {
			return false;
		}
	}
}
