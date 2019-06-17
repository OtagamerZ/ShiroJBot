package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Relay extends SQLite {
	private Map<String, String> relays = new HashMap<>();
	private int relaySize;
	private EmbedBuilder eb;

	private void checkSize() {
		if (relays.size() != relaySize) {
			relaySize = relays.size();
			Main.getJibril().getPresence().setGame(Game.listening("as mensagens de " + relaySize + " servidores!"));
		}
	}

	public void relayMessage(String msg, Member m, Guild s) {
		updateRelays();
		checkSize();

		eb = new EmbedBuilder();
		eb.setDescription(msg + "\n\n ");
		eb.setAuthor("(" + s.getName() + ") " + m.getEffectiveName(), s.getIconUrl(), s.getIconUrl());
		eb.setThumbnail(m.getUser().getAvatarUrl());
		eb.setFooter(m.getUser().getId(), "http://icons.iconarchive.com/icons/killaaaron/adobe-cc-circles/1024/Adobe-Id-icon.png");
		try {
			eb.setColor(Helper.colorThief(s.getIconUrl()));
		} catch (IOException e) {
			eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
		}

		StringBuilder badges = new StringBuilder();
		if (m.getUser().getId().equals(Main.getInfo().getNiiChan()) || Main.getInfo().getDevelopers().contains(m.getUser().getId()))
			badges.append("<:Dev:589103373354270760> ");

		if (Main.getInfo().getEditors().contains(m.getUser().getId()))
			badges.append("<:Editor:589120809428058123> ");

		try {
			if (MySQL.getTagById(m.getUser().getId()).isPartner())
				badges.append("<:Partner:589103374033485833> ");
		} catch (NoResultException ignore) {
		}

		if (m.hasPermission(Permission.MANAGE_CHANNEL))
			badges.append("<:Moderator:589121447314587744> ");

		try {
			if (MySQL.getChampionBeyblade().getId().equals(m.getUser().getId()))
				badges.append("<:Champion:589120809616932864> ");
		} catch (NoResultException ignore) {
		}

		try {
			if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 20)
				badges.append("<:Veteran:589121447151271976> ");
		} catch (NoResultException ignore) {
		}

		try {
			if (MySQL.getTagById(m.getUser().getId()).isToxic())
				badges.append("<:Toxic:589103372926451713> ");
		} catch (NoResultException ignore) {
		}

		eb.addField("Emblemas:", badges.toString(), false);

		relays.forEach((k, r) -> {
			if (!s.getId().equals(k) && m.getUser() != Main.getJibril().getSelfUser())
				try {
					Main.getJibril().getGuildById(k).getTextChannelById(r).sendMessage(eb.build()).queue();
				} catch (NullPointerException e) {
					SQLite.getGuildById(k).setCanalRelay(null);
				} catch (InsufficientPermissionException ex) {
					s.getOwner().getUser().openPrivateChannel().queue(c -> c.sendMessage(":x: | Me faltam permissões para enviar mensagens globais no servidor " + s.getName() + ".\n\nPermissões que eu possuo:```" +
							(s.getSelfMember().hasPermission(Permission.MESSAGE_WRITE) ? "✅" : "❌") + " Ler/Enviar mensagens\n" +
							(s.getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS) ? "✅" : "❌") + " Inserir links\n" +
							(s.getSelfMember().hasPermission(Permission.MESSAGE_ATTACH_FILES) ? "✅" : "❌") + " Anexar arquivos\n" +
							(s.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY) ? "✅" : "❌") + " Ver histórico de mensagens\n" +
							(s.getSelfMember().hasPermission(Permission.MESSAGE_EXT_EMOJI) ? "✅" : "❌") + " Usar emojis externos" +
					"```").queue());
				}
		});
	}

	public MessageEmbed getRelayInfo(guildConfig gc) {
		updateRelays();
		checkSize();
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(":globe_with_meridians: Dados do relay");
		eb.addField(":busts_in_silhouette: Clientes conectados: " + relays.size(), "Canal relay: " + (gc.getCanalRelay() == null ? "Não configurado" : Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalRelay()).getAsMention()), false);
		eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

		return eb.build();
	}

	public Map<String, String> getRelayMap() {
		updateRelays();
		checkSize();

		return relays;
	}

	@SuppressWarnings("unchecked")
	private void updateRelays() {
		EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);

		List<guildConfig> gc = q.getResultList();
		gc.removeIf(g -> g.getCanalRelay() == null || Main.getJibril().getGuildById(g.getGuildID()) == null);

		gc.forEach(g -> relays.put(g.getGuildID(), g.getCanalRelay()));
	}
}
