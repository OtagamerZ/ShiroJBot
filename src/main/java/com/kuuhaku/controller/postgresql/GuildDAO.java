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
import com.kuuhaku.model.persistent.guild.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

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
			if (gc == null) {
				Guild g = Main.getInfo().getGuildByID(id);
				if (g == null)
					return new GuildConfig(id);
				else
					return new GuildConfig(g.getId(), g.getName());
			}

			return gc;
		} finally {
			em.close();
		}
	}

	public static void removeGuildFromDB(GuildConfig gc) {
		if (gc.getGuildId() == null) return;
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.remove(em.contains(gc) ? gc : em.merge(gc));
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

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE autoExceedRoles = TRUE", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithPaidRoles() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE SIZE(paidRoles) > 0", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithButtons() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE SIZE(buttonConfigs) > 0", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithGeneralChannel() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE generalChannel <> ''", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAlertChannels() {
		EntityManager em = Manager.getEntityManager();

		Query gc = em.createQuery("SELECT g FROM GuildConfig g WHERE alertChannel <> ''", GuildConfig.class);
		List<GuildConfig> gcs = gc.getResultList();

		em.close();

		return gcs;
	}
}
