package com.kuuhaku.controller.sqlite;

import com.kuuhaku.model.guildConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class GuildDAO {
	public static guildConfig getGuildById(String id) {
		EntityManager em = Manager.getEntityManager();
		guildConfig gc;

		Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
		q.setParameter(1, id);
		gc = (guildConfig) q.getSingleResult();

		em.close();

		return gc;
	}

	public static void addGuildToDB(net.dv8tion.jda.api.entities.Guild guild) {
		EntityManager em = Manager.getEntityManager();

		guildConfig gc = new guildConfig();
		gc.setName(guild.getName());
		gc.setGuildId(guild.getId());

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeGuildFromDB(guildConfig gc) {
		EntityManager em = Manager.getEntityManager();

		gc.setMarkForDelete(true);

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}

	public static void updateGuildSettings(guildConfig gc) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();
	}
}
