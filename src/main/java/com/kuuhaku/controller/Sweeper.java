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

package com.kuuhaku.controller;

import com.kuuhaku.controller.postgresql.Manager;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Set;

public class Sweeper {
	public static void sweep(Set<String> guildIDs, Set<String> memberIDs) {
		EntityManager pem = Manager.getEntityManager();
		EntityManager sem = com.kuuhaku.controller.sqlite.Manager.getEntityManager();

		Query q;
		sem.getTransaction().begin();
		q = sem.createQuery("DELETE FROM GuildConfig WHERE guildID IN :ids");
		q.setParameter("ids", guildIDs);
		q.executeUpdate();

		q = sem.createQuery("DELETE FROM Member WHERE id IN :ids");
		q.setParameter("ids", memberIDs);
		q.executeUpdate();
		sem.getTransaction().commit();

		pem.getTransaction().begin();
		q = pem.createQuery("DELETE FROM GuildConfig WHERE guildID IN :ids");
		q.setParameter("ids", guildIDs);
		q.executeUpdate();

		q = pem.createQuery("DELETE FROM Member WHERE id IN :ids");
		q.setParameter("ids", memberIDs);
		q.executeUpdate();
		pem.getTransaction().commit();

		sem.close();
		pem.close();
	}
}
