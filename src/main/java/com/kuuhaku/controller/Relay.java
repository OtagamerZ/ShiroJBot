package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import com.kuuhaku.utils.TagIcons;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.webhook.WebhookCluster;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

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
	private WebhookCluster cluster = new WebhookCluster();

	private void checkSize() {
		if (relays.size() != relaySize) {
			relaySize = relays.size();
			Main.getJibril().getPresence().setGame(Game.listening("as mensagens de " + relaySize + " servidores!"));
		}
	}

	public void relayMessage(String msg, Member m, Guild s, String imgURL) {
		updateRelays();
		checkSize();

		eb = new EmbedBuilder();
		eb.setDescription(msg + "\n\n ");
		eb.setAuthor("(" + s.getName() + ") " + m.getEffectiveName(), s.getIconUrl(), s.getIconUrl());
		eb.setThumbnail(m.getUser().getAvatarUrl());
		if (imgURL != null) eb.setImage(imgURL);
		eb.setFooter(m.getUser().getId(), "http://icons.iconarchive.com/icons/killaaaron/adobe-cc-circles/1024/Adobe-Id-icon.png");
		try {
			eb.setColor(Helper.colorThief(s.getIconUrl()));
		} catch (IOException e) {
			eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
		}

		StringBuilder badges = new StringBuilder();
		if (m.getUser().getId().equals(Main.getInfo().getNiiChan()) || Main.getInfo().getDevelopers().contains(m.getUser().getId()))
			badges.append(TagIcons.getTag(TagIcons.DEV));

		if (Main.getInfo().getEditors().contains(m.getUser().getId()))
			badges.append(TagIcons.getTag(TagIcons.EDITOR));

		try {
			if (MySQL.getTagById(m.getUser().getId()).isPartner())
				badges.append(TagIcons.getTag(TagIcons.PARTNER));
		} catch (NoResultException ignore) {
		}

		if (m.hasPermission(Permission.MANAGE_CHANNEL))
			badges.append(TagIcons.getTag(TagIcons.MODERATOR));

		try {
			if (MySQL.getChampionBeyblade().getId().equals(m.getUser().getId()))
				badges.append(TagIcons.getTag(TagIcons.CHAMPION));
		} catch (NoResultException ignore) {
		}

		try {
			if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 20)
				badges.append(TagIcons.getTag(TagIcons.VETERAN));
		} catch (NoResultException ignore) {
		}

		try {
			if (MySQL.getTagById(m.getUser().getId()).isVerified())
				badges.append(TagIcons.getTag(TagIcons.VERIFIED));
		} catch (NoResultException ignore) {
		}

		try {
			if (MySQL.getTagById(m.getUser().getId()).isToxic())
				badges.append(TagIcons.getTag(TagIcons.TOXIC));
		} catch (NoResultException ignore) {
		}

		eb.addField("Emblemas:", badges.toString(), false);

		if (imgURL != null || Helper.findURL(msg))
			Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue()));

		relays.forEach((k, r) -> {
			if (!s.getId().equals(k))
				try {
					Webhook w = Helper.getOrCreateWebhook(Main.getJibril().getGuildById(k).getTextChannelById(r));
					if (w == null) return;
					cluster.addWebhooks(w.newClient().build());
				} catch (NullPointerException e) {
					SQLite.getGuildById(k).setCanalRelay(null);
				} catch (InsufficientPermissionException ex) {
					Main.getJibril().getGuildById(k).getOwner().getUser().openPrivateChannel().queue(c -> c.sendMessage(":x: | Me faltam permissões para enviar mensagens globais no servidor " + s.getName() + ".\n\nPermissões que eu possuo:```" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_WRITE) ? "✅" : "❌") + " Ler/Enviar mensagens\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS) ? "✅" : "❌") + " Inserir links\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_ATTACH_FILES) ? "✅" : "❌") + " Anexar arquivos\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_HISTORY) ? "✅" : "❌") + " Ver histórico de mensagens\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_EXT_EMOJI) ? "✅" : "❌") + " Usar emojis externos\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS) ? "✅" : "❌") + " Criar Webhooks" +
							"```").queue());
					Helper.log(this.getClass(), LogLevel.ERROR, ex.toString() + "\n" + k);
				}
		});

		WebhookMessageBuilder wmb = new WebhookMessageBuilder();
		wmb.setUsername("(" + s.getName() + ") " + m.getEffectiveName());
		wmb.setAvatarUrl(m.getUser().getAvatarUrl());
		wmb.addEmbeds(eb.build());

		cluster.setDefaultDaemon(true);
		cluster.broadcast(wmb.build());
		Helper.log(this.getClass(), LogLevel.INFO, cluster.getWebhooks().toString());
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
