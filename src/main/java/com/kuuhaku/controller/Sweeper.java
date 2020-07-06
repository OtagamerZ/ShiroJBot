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

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.Manager;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Member;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Sweeper {
	public static void sweep() {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		Query q = em.createQuery("DELETE FROM GuildConfig WHERE markForDelete = TRUE");
		q.executeUpdate();

		q = em.createQuery("DELETE FROM Member WHERE markForDelete = TRUE");
		q.executeUpdate();

		q = em.createQuery("DELETE FROM CustomAnswers WHERE markForDelete = TRUE");
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();

		em = com.kuuhaku.controller.sqlite.Manager.getEntityManager();

		em.getTransaction().begin();
		q = em.createQuery("DELETE FROM GuildConfig WHERE markForDelete = TRUE");
		q.executeUpdate();

		q = em.createQuery("DELETE FROM Member WHERE markForDelete = TRUE");
		q.executeUpdate();

		q = em.createQuery("DELETE FROM CustomAnswers WHERE markForDelete = TRUE");
		q.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	public static int mark() {
		List<GuildConfig> gcs = GuildDAO.getAllGuilds();
		List<Member> mbs = MemberDAO.getAllMembers();

		List<GuildConfig> safeGcs = gcs.stream()
				.filter(g -> Main.getInfo().getGuildByID(g.getGuildID()) != null)
				.collect(Collectors.toList());

		List<Member> safeMbs = new ArrayList<>();
		for (GuildConfig gc : safeGcs) {
			List<Member> found = safeMbs.stream()
					.filter(m -> Main.getInfo().getGuildByID(gc.getGuildID()).getMemberById(m.getMid()) != null)
					.collect(Collectors.toList());
			safeMbs.addAll(found);
		}

		gcs.removeAll(safeGcs);
		mbs.removeAll(safeMbs);

		gcs.forEach(gc -> gc.setMarkForDelete(true));
		mbs.forEach(mb -> mb.setMarkForDelete(true));

		safeGcs.forEach(gc -> gc.setMarkForDelete(false));
		safeMbs.forEach(mb -> mb.setMarkForDelete(false));

		EntityManager em = com.kuuhaku.controller.sqlite.Manager.getEntityManager();

		em.getTransaction().begin();
		gcs.forEach(em::merge);
		mbs.forEach(em::merge);
		safeGcs.forEach(em::merge);
		safeMbs.forEach(em::merge);
		em.getTransaction().commit();

		em.close();

		return gcs.size() + mbs.size();
	}
}
