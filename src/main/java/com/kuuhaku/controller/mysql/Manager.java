package com.kuuhaku.controller.mysql;

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

		if (emf == null) {
			emf = Persistence.createEntityManagerFactory("shiro_remote", props);
			Helper.logger(Manager.class).info("✅ | Ligação à base de dados MySQL estabelecida.");
		}

		emf.getCache().evictAll();

		return emf.createEntityManager();
	}
}
