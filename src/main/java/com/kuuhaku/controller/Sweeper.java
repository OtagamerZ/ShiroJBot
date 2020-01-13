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
import java.util.ArrayList;
import java.util.List;

public class Sweeper {
	public static int sweep() {
		EntityManager em = Manager.getEntityManager();

		int affected = 0;

		em.getTransaction().begin();
		Query q = em.createQuery("DELETE FROM GuildConfig WHERE markForDelete = TRUE");

		affected += q.executeUpdate();

		q = em.createQuery("DELETE FROM Member WHERE markForDelete = TRUE");
		affected += q.executeUpdate();
		em.getTransaction().commit();

		em.close();

		return affected;
	}

	public static void mark() {
		List<GuildConfig> gcs = GuildDAO.getAllGuilds();
		List<Member> mbs = MemberDAO.getAllMembers();

		List<Guild> gs = Main.getInfo().getAPI().getGuilds();
		List<net.dv8tion.jda.api.entities.Member> us = gs.stream().map(Guild::getMembers).collect(ArrayList::new, List::addAll, List::addAll);

		gcs.forEach(gc -> {
			if (gs.stream().noneMatch(g -> g.getId().equals(gc.getGuildID()))) gc.setMarkForDelete(true);
			else gc.setMarkForDelete(false);
			GuildDAO.updateGuildSettings(gc);
		});

		mbs.forEach(mb -> {
			if (us.stream().noneMatch(u -> (u.getId() + u.getGuild().getId()).equals(mb.getId()))) mb.setMarkForDelete(true);
			else mb.setMarkForDelete(false);
			MemberDAO.updateMemberConfigs(mb);
		});
	}
}
