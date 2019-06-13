package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Relay extends SQLite {
	private Map<String, String> relays = new HashMap<>();
	private EmbedBuilder eb;

	@SuppressWarnings("unchecked")
	public void relayMessage(String msg, Member m, Guild s) {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);

		List<guildConfig> gc = q.getResultList();
		gc.removeIf(g -> g.getCanalRelay() == null);

		if (gc.size() != relays.size()) {
			relays.clear();
			gc.forEach(g -> relays.put(g.getGuildID(), g.getCanalRelay()));
		}

		eb = new EmbedBuilder();
		eb.setAuthor("(" + s.getName() + ") " + m.getEffectiveName() + " disse:", m.getUser().getAvatarUrl(), m.getUser().getAvatarUrl());
		eb.setDescription(msg);
		try {
			eb.setColor(Helper.colorThief(m.getUser().getAvatarUrl()));
		} catch (IOException e) {
			eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
		}

		relays.forEach((k, r) -> {
			if (!s.getId().equals(k) && m.getUser() != Main.getInfo().getSelfUser())
				Main.getInfo().getAPI().getGuildById(k).getTextChannelById(r).sendMessage(eb.build()).queue();
		});
	}

	public MessageEmbed getRelayInfo(guildConfig gc) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(":globe_with_meridians: Dados do relay");
		eb.addField(":busts_in_silhouette: Clientes conectados: " + relays.size(), "", false);
		eb.addField("Canal relay: " + (gc.getCanalRelay() == null ? "Não configurado" : Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalRelay()).getName()), "", false);
		eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

		return eb.build();
	}
}
