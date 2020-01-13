package com.kuuhaku.controller.sqlite;

import com.kuuhaku.model.GuildConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class GuildDAO {
	public static GuildConfig getGuildById(String id) {
		EntityManager em = Manager.getEntityManager();
		GuildConfig gc;

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE guildID = ?1", GuildConfig.class);
		q.setParameter(1, id);
		gc = (GuildConfig) q.getSingleResult();

		em.close();

		return gc;
	}



	public static void addGuildToDB(net.dv8tion.jda.api.entities.Guild guild) {
		EntityManager em = Manager.getEntityManager();

		GuildConfig gc = new GuildConfig();
		gc.setName(guild.getName());
		gc.setGuildId(guild.getId());

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeGuildFromDB(GuildConfig gc) {
		EntityManager em = Manager.getEntityManager();

		gc.setMarkForDelete(true);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void updateGuildSettings(GuildConfig gc) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuilds() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		List<GuildConfig> gcs = (List<GuildConfig>) gc.getResultList();

		em.close();

		return gcs;
	}
}
