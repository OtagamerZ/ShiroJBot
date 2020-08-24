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

import com.kuuhaku.model.persistent.Account;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

public class AccountDAO {
	@SuppressWarnings("unchecked")
	public static List<Account> getNotifiableAccounts() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a WHERE remind = TRUE", Account.class);

		try {
			return (List<Account>) q.getResultList();
		} finally {
			em.close();
		}
	}

	public static Account getAccount(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a WHERE userId = :id", Account.class);
		q.setParameter("id", id);

		try {
			return (Account) q.getSingleResult();
		} catch (NoResultException e) {
			Account acc = new Account();
			acc.setUserId(id);
			saveAccount(acc);
			return acc;
		} finally {
			em.close();
		}
	}

	public static Account getAccountByTwitchId(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a WHERE twitchId = :id", Account.class);
		q.setParameter("id", id);

		try {
			return (Account) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static void saveAccount(Account acc) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(acc);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Account> getAccountRank() {
		EntityManager em = com.kuuhaku.controller.sqlite.Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a ORDER BY a.balance DESC", Account.class);

		List<Account> accs = (List<Account>) q.getResultList();

		em.close();

		return accs;
	}
}
