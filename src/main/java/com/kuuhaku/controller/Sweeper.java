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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

	public static int mark() {
		List<GuildConfig> gcs = GuildDAO.getAllGuilds();
		List<Member> mbs = MemberDAO.getAllMembers();

		Map<String, List<net.dv8tion.jda.api.entities.Member>> gs = Main.getInfo().getAPI().getGuilds().stream().collect(Collectors.toMap(Guild::getId, Guild::getMembers));

		System.out.println(Arrays.toString(gs.keySet().toArray()));

		gcs.removeIf(g -> gs.containsKey(g.getGuildID()));
		mbs.removeIf(m -> {
			List<net.dv8tion.jda.api.entities.Member> ms = gs.getOrDefault(m.getSid(), null);
			if (ms == null) return false;
			else return ms.stream().anyMatch(c -> c.getId().equals(m.getMid()));
		});

		gcs.forEach(gc -> {
			gc.setMarkForDelete(true);
			GuildDAO.updateGuildSettings(gc);
		});

		mbs.forEach(mb -> {
			mb.setMarkForDelete(true);
			MemberDAO.updateMemberConfigs(mb);
		});

		return gcs.size() + mbs.size();
	}
}
