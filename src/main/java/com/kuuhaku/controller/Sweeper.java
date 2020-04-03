/*
 * This file is part of Shiro J Bot.
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
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
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

		Map<String, List<net.dv8tion.jda.api.entities.Member>> gs = Main.getInfo().getAPI().getGuilds().stream().collect(Collectors.toMap(Guild::getId, Guild::getMembers));

		List<GuildConfig> safeGcs = gcs.stream().filter(g -> gs.containsKey(g.getGuildID())).collect(Collectors.toList());
		List<Member> safeMbs = mbs.stream().filter(m -> gs.containsKey(m.getSid()) && gs.get(m.getSid()).stream().anyMatch(mb -> mb.getId().equals(m.getMid()))).collect(Collectors.toList());

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
