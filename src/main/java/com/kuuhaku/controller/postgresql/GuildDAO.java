/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

		Query q = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
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
		em.createQuery("DELETE FROM GuildConfig gc WHERE gc.guildId = :id")
				.setParameter("id", gc.getGuildId())
				.executeUpdate();
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
	public static List<GuildConfig> getAllGuildsWithPaidRoles() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE SIZE(g.paidRoles) > 0", GuildConfig.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithVoiceRoles() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE SIZE(g.voiceRoles) > 0", GuildConfig.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithButtons() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE SIZE(g.buttonConfigs) > 0", GuildConfig.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAllGuildsWithGeneralChannel() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE g.generalChannel <> ''", GuildConfig.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<GuildConfig> getAlertChannels() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE g.alertChannel <> ''", GuildConfig.class);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}
}
