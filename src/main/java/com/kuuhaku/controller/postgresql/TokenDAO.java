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
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.Token;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Member;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class TokenDAO {
	public static Token getToken(String token) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE token = :token", Token.class);
		q.setParameter("token", token);
		q.setMaxResults(1);

		try {
			return (Token) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Token getTokenById(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE uid = :id", Token.class);
		q.setParameter("id", id);
		q.setMaxResults(1);

		try {
			return (Token) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Token registerToken(String id) {
		EntityManager em = Manager.getEntityManager();

		Token t = new Token(Helper.generateToken(id, 48), Main.getInfo().getUserByID(id).getAsTag(), id);

		em.getTransaction().begin();
		em.merge(t);
		em.getTransaction().commit();

		em.close();

		return t;
	}

	public static String verifyToken(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE uid = :uid", Token.class);
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

			boolean allowed = false;
			for (Member m : Main.getInfo().getMembersByID(t.getUid())) {
				if (Helper.hasPermission(m, PrivilegeLevel.BETA)) {
					allowed = true;
					break;
				}
			}

			if (!allowed) return null;
			else return t.getToken();
		} catch (NoResultException e) {
			Token t = registerToken(id);
			return t.getToken();
		}
	}

	public static boolean validateToken(String token) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT t FROM Token t WHERE token = :token AND disabled = FALSE", Token.class);
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
