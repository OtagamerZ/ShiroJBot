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
