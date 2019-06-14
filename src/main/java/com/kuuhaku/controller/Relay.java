package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Relay extends SQLite {
	private Map<String, String> relays = new HashMap<>();
	private EmbedBuilder eb;

	public void relayMessage(String msg, Member m, Guild s) throws Exception {
		updateRelays();

		eb = new EmbedBuilder();
		eb.setDescription(msg);
		eb.setAuthor("(" + s.getName() + ") " + m.getEffectiveName() + " disse:", s.getIconUrl(), s.getIconUrl());
		eb.setFooter(m.getUser().getId(), m.getUser().getAvatarUrl());
		try {
			eb.setColor(Helper.colorThief(m.getUser().getAvatarUrl()));
		} catch (IOException e) {
			eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
		}

		StringBuilder badges = new StringBuilder();
		if (m.getUser().getId().equals(Main.getInfo().getNiiChan()) || Main.getInfo().getDevelopers().contains(m.getUser().getId()))
			badges.append("<:Dev:589103373354270760> ");

		if (Main.getInfo().getEditors().contains(m.getUser().getId()))
			badges.append("<:Editor:589120809428058123> ");

		try {
			if (SQLite.getTagById(m.getUser().getId()).isPartner())
				badges.append("<:Partner:589103374033485833> ");
		} catch (NoResultException ignore) { }

		if (m.hasPermission(Permission.MANAGE_CHANNEL))
			badges.append("<:Moderator:589121447314587744> ");

		try {
			if (MySQL.getChampionBeyblade().getId().equals(m.getUser().getId()))
			badges.append("<:Champion:589120809616932864> ");
		} catch (NoResultException ignore) { }

		try {
		if (SQLite.getMemberById(m.getUser().getId()).getLevel() >= 20)
			badges.append("<:Veteran:589121447151271976> ");
		} catch (NoResultException ignore) { }

		try {
		if (SQLite.getTagById(m.getUser().getId()).isToxic())
			badges.append("<:Toxic:589103372926451713> ");
		} catch (NoResultException ignore) { }

		eb.setTitle("" + badges.toString());

		relays.forEach((k, r) -> {
			if (!s.getId().equals(k) && m.getUser() != Main.getInfo().getSelfUser())
				Main.getInfo().getAPI().getGuildById(k).getTextChannelById(r).sendMessage(eb.build()).queue();
		});
	}

	public MessageEmbed getRelayInfo(guildConfig gc) {
		updateRelays();
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(":globe_with_meridians: Dados do relay");
		eb.addField(":busts_in_silhouette: Clientes conectados: " + relays.size(), "Canal relay: " + (gc.getCanalRelay() == null ? "Não configurado" : Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalRelay()).getAsMention()), false);
		eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

		return eb.build();
	}

	public List<String> getRelayArray() {
		updateRelays();
		List<String> ids = new ArrayList<>();
		relays.forEach((k, v) -> ids.add(k));

		return ids;
	}

	@SuppressWarnings("unchecked")
	private void updateRelays() {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);

		List<guildConfig> gc = q.getResultList();
		gc.removeIf(g -> g.getCanalRelay() == null);

		gc.forEach(g -> relays.put(g.getGuildID(), g.getCanalRelay()));
	}
}
