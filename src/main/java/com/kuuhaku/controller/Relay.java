package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import com.kuuhaku.utils.TagIcons;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Relay extends SQLite {
	private final Map<String, String> relays = new HashMap<>();
	private int relaySize;

	private void checkSize() {
		if (relays.size() != relaySize) {
			relaySize = relays.size();
			Main.getJibril().getPresence().setGame(Game.listening("as mensagens de " + relaySize + " servidores!"));
		}
	}

	private WebhookMessage getMessage(String msg, Member m, Guild s) {
		WebhookMessageBuilder wmb = new WebhookMessageBuilder();

		String filtered = Arrays.stream(msg.split(" ")).map(w -> w =
				(w.contains("<") && w.contains(">") && w.contains(":")) ? ":question:" : w
		).collect(Collectors.joining(" "));
		wmb.setContent(filtered);
		wmb.setAvatarUrl(RelayBlockList.checkThumb(m.getUser().getId()) ? "https://i.pinimg.com/originals/46/15/87/461587d51087bfdf8906149d356f972f.jpg" : m.getUser().getAvatarUrl());
		wmb.setUsername("(" + s.getName() + ") " + m.getEffectiveName());
		return wmb.build();
	}

	private WebhookClient getClient(TextChannel ch, Guild s) {
		List<Webhook> wbs = ch.getWebhooks().complete().stream().filter(w -> w.getOwner() == s.getSelfMember()).collect(Collectors.toList());
		if (wbs.size() != 0) return wbs.get(0).newClient().build();
		else return Objects.requireNonNull(Helper.getOrCreateWebhook(ch)).newClient().build();
	}

	public void relayMessage(Message source, String msg, Member m, Guild s, ByteArrayOutputStream img) {
		updateRelays();
		checkSize();
		try {
			if (!SQLite.getGuildById(s.getId()).isLiteMode()) {
				source.delete().queue();
			}
		} catch (InsufficientPermissionException ignore) {
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(Helper.makeEmoteFromMention(msg.split(" ")) + "\n\n ");
		eb.setImage("attachment://image.png");
		eb.setAuthor("(" + s.getName() + ") " + m.getEffectiveName(), s.getIconUrl(), s.getIconUrl());
		eb.setThumbnail(RelayBlockList.checkThumb(m.getUser().getId()) ? "https://i.pinimg.com/originals/46/15/87/461587d51087bfdf8906149d356f972f.jpg" : m.getUser().getAvatarUrl());
		eb.setFooter(m.getUser().getId(), "http://icons.iconarchive.com/icons/killaaaron/adobe-cc-circles/1024/Adobe-Id-icon.png");
		try {
			eb.setColor(Helper.colorThief(s.getIconUrl()));
		} catch (IOException e) {
			eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
		}

		StringBuilder badges = new StringBuilder();
		if (!SQLite.getMemberByMid(m.getUser().getId()).getExceed().isEmpty()) {
			badges.append(TagIcons.getExceed(ExceedEnums.getByName(SQLite.getMemberByMid(m.getUser().getId()).getExceed())));
		}

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
			if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 60)
				badges.append(TagIcons.getTag(TagIcons.LVL60));
			else if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 50)
				badges.append(TagIcons.getTag(TagIcons.LVL50));
			else if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 40)
				badges.append(TagIcons.getTag(TagIcons.LVL40));
			else if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 30)
				badges.append(TagIcons.getTag(TagIcons.LVL30));
			else if (SQLite.getMemberById(m.getUser().getId() + s.getId()).getLevel() >= 20)
				badges.append(TagIcons.getTag(TagIcons.LVL20));
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

		try {
			if (!SQLite.getMemberById(m.getUser().getId() + s.getId()).getWaifu().isEmpty())
				badges.append(TagIcons.getTag(TagIcons.MARRIED));
		} catch (NoResultException ignore) {
		}

		eb.addField("Emblemas:", badges.toString(), false);

		MessageBuilder mb = new MessageBuilder();
		mb.setEmbed(eb.build());

		relays.forEach((k, r) -> {
			if (k.equals(s.getId()) && SQLite.getGuildById(k).isLiteMode()) return;
			try {
				if (SQLite.getGuildById(k).isAllowImg()) {
					if (SQLite.getGuildById(k).isLiteMode()) {
						WebhookClient client = getClient(Main.getJibril().getGuildById(k).getTextChannelById(r), Main.getJibril().getGuildById(k));
						client.send(getMessage(msg, m, s));
						client.close();
					} else {
						if (img != null) {
							Main.getJibril().getGuildById(k).getTextChannelById(r).sendFile(img.toByteArray(), "image.png", mb.build()).queue();
						} else {
							Main.getJibril().getGuildById(k).getTextChannelById(r).sendMessage(mb.build()).queue();
						}
					}
				} else {
					if (SQLite.getGuildById(k).isLiteMode()) {
						WebhookClient client = getClient(Main.getJibril().getGuildById(k).getTextChannelById(r), Main.getJibril().getGuildById(k));
						client.send(getMessage(msg, m, s));
						client.close();
					} else {
						Main.getJibril().getGuildById(k).getTextChannelById(r).sendMessage(mb.build()).queue();
					}
				}
			} catch (NullPointerException e) {
				SQLite.getGuildById(k).setCanalRelay(null);
			} catch (InsufficientPermissionException ex) {
				try {
					Main.getJibril().getGuildById(k).getOwner().getUser().openPrivateChannel().queue(c -> c.sendMessage(":x: | Me faltam permissões para enviar mensagens globais no servidor " + s.getName() + ".\n\nPermissões que eu possuo:```" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_WRITE) ? "✅" : "❌") + " Ler/Enviar mensagens\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS) ? "✅" : "❌") + " Inserir links\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_ATTACH_FILES) ? "✅" : "❌") + " Anexar arquivos\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_HISTORY) ? "✅" : "❌") + " Ver histórico de mensagens\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_EXT_EMOJI) ? "✅" : "❌") + " Usar emojis externos\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) ? "✅" : "❌") + " Gerenciar mensagens\n" +
							(Main.getJibril().getGuildById(k).getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS) ? "✅" : "❌") + " Gerenciar webhooks" +
							"```").queue());
					Helper.log(this.getClass(), LogLevel.ERROR, ex + " | Sevidor " + Main.getJibril().getGuildById(k).getName());
				} catch (Exception e) {
					Helper.log(this.getClass(), LogLevel.ERROR, ex + " | Dono " + Main.getJibril().getGuildById(k).getOwner().getUser().getAsTag());
				}
			}
		});
	}

	public MessageEmbed getRelayInfo(guildConfig gc) {
		updateRelays();
		checkSize();
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(":globe_with_meridians: Dados do relay");
		eb.addField(":busts_in_silhouette: Clientes conectados: " + relays.size(), "Canal relay: " + (gc.getCanalRelay() == null ? "Não configurado" : Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalRelay()).getAsMention()), false);
		eb.addField("Modo:", gc.isLiteMode() ? "Lite" : "Normal", true);
		eb.addField("Imagens:", gc.isAllowImg() ? "Permitidas" : "Negadas", true);
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
