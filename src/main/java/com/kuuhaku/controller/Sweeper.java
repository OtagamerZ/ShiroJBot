package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.Manager;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.GuildConfig;
import com.kuuhaku.model.Member;
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
		em.getTransaction().commit();

		em.close();
	}

	public static int mark() {
		List<GuildConfig> gcs = GuildDAO.getAllGuilds();
		List<Member> mbs = MemberDAO.getAllMembers();

		Map<String, List<net.dv8tion.jda.api.entities.Member>> gs = Main.getInfo().getAPI().getGuilds().stream().collect(Collectors.toMap(Guild::getId, Guild::getMembers));

		List<GuildConfig> safeGcs = gcs.stream().filter(g -> gs.containsKey(g.getGuildID())).collect(Collectors.toList());
		List<Member> safeMbs = mbs.stream().filter(m -> {
			List<net.dv8tion.jda.api.entities.Member> ms = gs.getOrDefault(m.getSid(), null);
			if (ms == null) return false;
			else return ms.stream().anyMatch(c -> c.getId().equals(m.getMid()));
		}).collect(Collectors.toList());

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

		return gcs.size() + mbs.size();
	}
}
