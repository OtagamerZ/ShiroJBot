package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.GuildConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class GuildDAO {
	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuilds() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		List<GuildConfig> gcs = (List<GuildConfig>) gc.getResultList();

		em.close();

		return gcs;
	}
}
