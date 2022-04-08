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

package com.kuuhaku.controller;

import com.kuuhaku.Constants;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;

public class Manager {
	private static final String SERVER_IP = System.getenv("SERVER_IP");
	private static final String DB_NAME = System.getenv("DB_NAME");
	private static final String DB_LOGIN = System.getenv("DB_LOGIN");
	private static final String DB_PASS = System.getenv("DB_PASS");

	private static EntityManagerFactory emf;

	public synchronized static EntityManager getEntityManager() {
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory("main", Map.of(
					"javax.persistence.jdbc.user", DB_LOGIN,
					"javax.persistence.jdbc.password", DB_PASS,
					"javax.persistence.jdbc.url", "jdbc:postgresql://%s/%s?sslmode=require".formatted(
							SERVER_IP, DB_NAME
					)
			));
			Constants.LOGGER.info("Connected to database sucessfully");
		}

		return emf.createEntityManager();
	}

	public static long ping() {
		getEntityManager().close();

		long curr = System.currentTimeMillis();

		DAO.queryUnmapped("SELECT 1");

		return System.currentTimeMillis() - curr;
	}
}
