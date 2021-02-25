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

import com.kuuhaku.model.persistent.TokenTransaction;
import com.kuuhaku.model.persistent.Transaction;

import javax.persistence.EntityManager;

public class TransactionDAO {
	public static void register(String user, Class<?> from, long value) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(new Transaction(user, from.getSimpleName(), value));
		em.getTransaction().commit();

		em.close();
	}

	public static void register(String to, String from, int value) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(new TokenTransaction(to, from, value));
		em.getTransaction().commit();

		em.close();
	}
}
