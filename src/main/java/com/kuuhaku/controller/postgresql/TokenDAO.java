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

import com.kuuhaku.Main;
import com.kuuhaku.model.persistent.Token;
import org.apache.commons.lang3.ArrayUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class TokenDAO {
	public static Token registerToken(String id) {
		EntityManager em = Manager.getEntityManager();

		SecureRandom sr = new SecureRandom();
		byte[] nameSpace = id.getBytes(StandardCharsets.UTF_8);
		byte[] randomSpace = new byte[48 - nameSpace.length];
		sr.nextBytes(randomSpace);

		Token t = new Token(Base64.getEncoder().encodeToString(ArrayUtils.addAll(nameSpace, randomSpace)), Main.getInfo().getUserByID(id).getAsTag(), id);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();

		return t;
	}

	public static String verifyToken(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE uid LIKE :uid", Token.class);
		q.setParameter("uid", id);
		q.setMaxResults(1);

		try {
			Token t = (Token) q.getSingleResult();

			if (t.isDisabled()) {
				return null;
			}

			em.getTransaction().begin();
			em.merge(t.addCall());
			em.getTransaction().commit();

			em.close();

			return t.getToken();
		} catch (NoResultException e) {
			Token t = registerToken(id);
			return t.getToken();
		}
	}

	public static boolean validateToken(String token) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE token LIKE :token AND NOT disabled", Token.class);
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
