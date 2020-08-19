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

import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

public class Manager {
	private static EntityManagerFactory emf;

	public static EntityManager getEntityManager() {
		Map<String, String> props = new HashMap<>();
		props.put("javax.persistence.jdbc.user", System.getenv("DB_LOGIN"));
		props.put("javax.persistence.jdbc.password", System.getenv("DB_PASS"));
		props.put("javax.persistence.jdbc.url", "jdbc:postgresql://" + System.getenv("SERVER_IP") + "/shiro?serverTimezone=EST5EDT&autoReconnect=true&useUnicode=yes&characterEncoding=UTF-8&interactiveClient=true&reWriteBatchedInserts=true");

		if (emf == null) {
			emf = Persistence.createEntityManagerFactory("shiro_remote", props);
			Helper.logger(Manager.class).info("✅ | Ligação à base de dados PostgreSQL estabelecida.");
		}

		emf.getCache().evictAll();

		return emf.createEntityManager();
	}
}
