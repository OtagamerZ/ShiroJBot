package com.kuuhaku.controller.MySQL;

import com.kuuhaku.model.guildConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class GuildDAO {
	@SuppressWarnings("unchecked")
	public static List<guildConfig> getAllGuilds() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);
		List<guildConfig> gcs = (List<guildConfig>) gc.getResultList();

		em.close();

		return gcs;
	}
}
