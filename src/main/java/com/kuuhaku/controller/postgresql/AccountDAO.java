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
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {
	@SuppressWarnings("unchecked")
	public static List<Account> getNotifiableAccounts() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a WHERE remind = TRUE", Account.class);
		q.setLockMode(LockModeType.PESSIMISTIC_READ);

		try {
			em.getTransaction().begin();
			return q.getResultList();
		} finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Account> getVolatileAccounts() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a WHERE vBalance > 0", Account.class);
		q.setLockMode(LockModeType.PESSIMISTIC_READ);

		try {
			em.getTransaction().begin();
			return q.getResultList();
		} finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	public static Account getAccount(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();
			Account acc = em.find(Account.class, id, LockModeType.PESSIMISTIC_READ);
			if (acc == null) {
				acc = new Account();
				acc.setUid(id);
				return acc;
			}

			return acc;
		} finally {
			em.close();
			em.getTransaction().commit();
		}
	}

	public static Account getAccountByTwitchId(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a WHERE twitchId = :id", Account.class);
		q.setLockMode(LockModeType.PESSIMISTIC_READ);
		q.setParameter("id", id);

		try {
			em.getTransaction().begin();
			return (Account) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	public static void saveAccount(Account acc) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		if (em.contains(acc))
			em.lock(acc, LockModeType.PESSIMISTIC_WRITE);
		em.merge(acc);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeAccount(Account acc) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		if (em.contains(acc))
			em.lock(acc, LockModeType.PESSIMISTIC_WRITE);
		em.remove(acc);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<Account> getAccountRank() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a ORDER BY a.balance DESC", Account.class);
		q.setLockMode(LockModeType.PESSIMISTIC_READ);

		try {
			em.getTransaction().begin();
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	public static void addSupportTokens() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM Account a ORDER BY a.balance DESC", Account.class);
		q.setLockMode(LockModeType.PESSIMISTIC_WRITE);

		em.getTransaction().begin();
		em.createQuery("""
				UPDATE Account a 
				SET a.thanksTokens = CASE 
					WHEN (a.thanksTokens > 3) THEN 3
					ELSE a.thanksTokens
				END + 1
				""").executeUpdate();
		em.getTransaction().commit();
		em.close();
	}
}
