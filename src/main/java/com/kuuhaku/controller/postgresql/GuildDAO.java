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

import com.kuuhaku.Main;
import com.kuuhaku.model.persistent.GuildConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

public class GuildDAO {
	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuilds() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	public static GuildConfig getGuildById(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			GuildConfig gc = em.find(GuildConfig.class, id);
			if (gc == null)
				return addGuildToDB(Main.getInfo().getGuildByID(id));

			return gc;
		} finally {
			em.close();
		}
	}

	public static GuildConfig addGuildToDB(net.dv8tion.jda.api.entities.Guild guild) {
		EntityManager em = Manager.getEntityManager();

		GuildConfig gc = new GuildConfig();
		gc.setName(guild.getName());
		gc.setGuildId(guild.getId());

		em.getTransaction().begin();
		em.merge(gc);
		em.getTransaction().commit();

		em.close();

		return gc;
	}

	public static void removeGuildFromDB(GuildConfig gc) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(gc);
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
	public static List<GuildConfig> getAllGuildsWithExceedRoles() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE exceedRolesEnabled = TRUE", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithButtons() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE COALESCE(buttonConfigs, '') NOT IN ('', '{}')", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithGeneralChannel() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE canalGeral <> ''", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAlertChannels() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE COALESCE(canalAvisos,'') <> ''", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static void updateRelays(Map<String, String> relays) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE canalrelay <> '' AND canalrelay IS NOT NULL", GuildConfig.class);

		List<GuildConfig> gc = q.getResultList();
		gc.removeIf(g -> Main.getJibril().getGuildById(g.getGuildID()) == null);
		for (GuildConfig g : gc) {
			relays.put(g.getGuildID(), g.getCanalRelay());
		}

		em.close();
	}
}
