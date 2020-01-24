/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.controller.sqlite;

import com.kuuhaku.Main;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Manager {

	private static EntityManagerFactory emf;

	public static void connect() {

		File DBfile = new File(Main.getInfo().getDBFileName());
		if (!DBfile.exists()) {
			Helper.logger(Manager.class).fatal("A base de dados não foi encontrada. Entre no servidor discord oficial da Shiro para obter ajuda.");
			System.exit(1);
		}

		Map<String, String> props = new HashMap<>();
		props.put("javax.persistence.jdbc.url", "jdbc:sqlite:" + DBfile.getPath());

		if (emf == null) emf = Persistence.createEntityManagerFactory("shiro_local", props);

		emf.getCache().evictAll();
	}

	public static EntityManager getEntityManager() {
		if (emf == null) connect();
		return emf.createEntityManager();
	}

	public static void disconnect() {
		if (emf != null) {
			emf.close();
			Helper.logger(Manager.class).info("Ligação à base de dados desfeita.");
		}
	}
}
