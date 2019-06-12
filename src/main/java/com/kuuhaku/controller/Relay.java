package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Relay extends SQLite {
	@SuppressWarnings("unchecked")
	public static void relayMessage(String msg) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);

		List<guildConfig> gc = q.getResultList();
		Map<String, String> relays = new HashMap<>();

		gc.forEach(g -> relays.put(g.getGuildID(), g.getCanalrelay()));

		relays.forEach((k, r) -> Main.getInfo().getAPI().getGuildById(k).getTextChannelById(r).sendMessage(msg).queue());
	}
}
