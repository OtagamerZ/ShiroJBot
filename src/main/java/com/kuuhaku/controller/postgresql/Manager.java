/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;

public class Manager {
	private static EntityManagerFactory emf = null;

	public static EntityManager getEntityManager() {
		if (emf == null) {
			Map<String, String> props = new HashMap<>();
			props.put("javax.persistence.jdbc.user", System.getenv("DB_LOGIN"));
			props.put("javax.persistence.jdbc.password", System.getenv("DB_PASS"));
			props.put("javax.persistence.jdbc.url", "jdbc:postgresql://" + System.getenv("SERVER_IP") + "/shiro?sslmode=require");

			emf = Persistence.createEntityManagerFactory("shiro_remote", props);
			Helper.logger(Manager.class).info("✅ | Ligação à base de dados PostgreSQL estabelecida.");
		}

		return emf.createEntityManager();
	}

	public static long ping() {
		long curr = System.currentTimeMillis();
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT v FROM Version v");

		q.getSingleResult();
		em.close();

		return System.currentTimeMillis() - curr;
	}
}
