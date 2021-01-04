/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.GlobalMessageDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.Manager;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.RelayBlockList;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.Tag;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.GlobalMessage;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Relay {
	private final Map<String, String> relays = new HashMap<>();
	private int relaySize;

	private void checkSize() {
		if (relays.size() != relaySize) {
			relaySize = relays.size();
			Main.getJibril().getPresence().setActivity(Activity.listening("as mensagens de " + relaySize + " servidores!"));
		}
	}

	private WebhookMessage getMessage(String msg, Member m, Guild s) {
		WebhookMessageBuilder wmb = new WebhookMessageBuilder();
		String exceed = ExceedDAO.getExceed(m.getId());

		String filtered = Arrays.stream(msg.split(" ")).map(w -> w =
				(w.contains("<") && w.contains(">") && w.contains(":")) ? ":question:" : w
		).collect(Collectors.joining(" "));
		wmb.setContent(filtered);
		wmb.setAvatarUrl(RelayBlockList.checkThumb(m.getUser().getId()) ? "https://i.pinimg.com/originals/46/15/87/461587d51087bfdf8906149d356f972f.jpg" : m.getUser().getAvatarUrl());
		wmb.setUsername("(" + s.getName() + ") " + (exceed.isEmpty() ? "" : "[" + exceed + "] ") + (m.getUser().getName().length() > 15 ? m.getUser().getName().substring(0, 15) + "..." : m.getUser().getName()));
		return wmb.build();
	}

	private WebhookMessage getMessage(GlobalMessage gm) {
		WebhookMessageBuilder wmb = new WebhookMessageBuilder();

		wmb.setContent(gm.getContent());
		wmb.setAvatarUrl(RelayBlockList.checkThumb(gm.getUserId()) ? "https://i.pinimg.com/originals/46/15/87/461587d51087bfdf8906149d356f972f.jpg" : gm.getAvatar());
		wmb.setUsername(gm.getName().length() > 15 ? gm.getName().substring(0, 15) + "..." : gm.getName());
		return wmb.build();
	}

	private WebhookClient getClient(TextChannel ch, Guild s) {
		List<Webhook> wbs = ch.retrieveWebhooks().complete().stream().filter(w -> w.getOwner() == s.getSelfMember()).collect(Collectors.toList());
		if (wbs.size() != 0) {
			WebhookClientBuilder wcb = new WebhookClientBuilder(wbs.get(0).getUrl());
			return wcb.build();
		} else {
			try {
				WebhookClientBuilder wcb = new WebhookClientBuilder(Objects.requireNonNull(Helper.getOrCreateWebhook(ch, "Jibril", Main.getJibril())).getUrl());
				return wcb.build();
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException e) {
				Helper.sendPM(Objects.requireNonNull(ch.getGuild().getOwner()).getUser(), "❌ | " + Main.getJibril().getSelfUser().getName() + " não possui permissão para criar um webhook em seu servidor no canal " + ch.getAsMention());
				return null;
			}
		}
	}

	public void relayMessage(Message source, String msg, Member m, Guild s, ByteArrayOutputStream img) {
		updateRelays();
		checkSize();

		GlobalMessage gm = new GlobalMessage();
		gm.setUserId(m.getId());
		gm.setName(m.getUser().getName());
		gm.setAvatar(m.getUser().getAvatarUrl());
		gm.setContent(msg);

		Main.getInfo().getSockets().getChat().onMessage(null, gm.toString());
		GlobalMessageDAO.saveMessage(gm);

		String exceed = ExceedDAO.getExceed(m.getId());

		EmbedBuilder eb;
		if (s.getIconUrl() != null) eb = new EmbedBuilder();
		else eb = new ColorlessEmbedBuilder();
		eb.setDescription(Helper.makeEmoteFromMention(msg.split(" ")) + "\n\n");
		eb.setImage("attachment://image.png");
		eb.setAuthor("(" + s.getName() + ") " + (exceed.isEmpty() ? "" : "[" + exceed + "] ") + m.getUser().getName(), s.getIconUrl(), s.getIconUrl());
		eb.setThumbnail(RelayBlockList.checkThumb(m.getUser().getId()) ? "https://i.pinimg.com/originals/46/15/87/461587d51087bfdf8906149d356f972f.jpg" : m.getUser().getAvatarUrl());
		eb.setFooter(m.getUser().getId(), "http://icons.iconarchive.com/icons/killaaaron/adobe-cc-circles/1024/Adobe-Id-icon.png");
		try {
			eb.setColor(Helper.colorThief(s.getIconUrl()));
		} catch (IOException ignore) {
		}

		StringBuilder badges = new StringBuilder();
		Set<Tag> tags = Tag.getTags(m.getUser(), m);
		com.kuuhaku.model.persistent.Member mbr = MemberDAO.getMemberById(m.getId() + s.getId());

		if (!exceed.isEmpty()) {
			badges.append(TagIcons.getExceed(ExceedEnum.getByName(exceed)));
			for (Tag t : tags) {
				badges.append(t.getEmote(mbr) == null ? "" : t.getEmote(mbr).getTag(mbr.getLevel()));
			}
		} else {
			for (Tag t : tags) {
				badges.append(t.getEmote(mbr) == null ? "" : t.getEmote(mbr).getTag(mbr.getLevel()));
			}
		}

		eb.addField("Emblemas:", badges.toString(), false);

		MessageBuilder mb = new MessageBuilder();
		mb.setEmbed(eb.build());

		for (Map.Entry<String, String> entry : relays.entrySet()) {
			String k = entry.getKey();
			String r = entry.getValue();
			if (k.equals(s.getId()) && GuildDAO.getGuildById(k).isLiteMode() && m.getUser() != Main.getJibril().getSelfUser())
				continue;
			try {
				TextChannel t = Objects.requireNonNull(Main.getJibril().getGuildById(k)).getTextChannelById(r);
				assert t != null;
				if (GuildDAO.getGuildById(k).isAllowImg()) {
					if (GuildDAO.getGuildById(k).isLiteMode()) {
						WebhookClient client = getClient(t, Main.getJibril().getGuildById(k));
						if (client != null) {
							client.send(getMessage(msg, m, s));
							client.close();
						}
					} else {
						if (img != null) {
							t.sendMessage(mb.build()).addFile(img.toByteArray(), "image.png").queue();
						} else {
							t.sendMessage(mb.build()).queue();
						}
					}
				} else {
					if (GuildDAO.getGuildById(k).isLiteMode()) {
						WebhookClient client = getClient(t, Main.getJibril().getGuildById(k));
						if (client != null) {
							client.send(getMessage(msg, m, s));
							client.close();
						}
					} else {
						t.sendMessage(mb.build()).queue();
					}
				}
			} catch (NullPointerException e) {
				GuildDAO.getGuildById(k).setCanalRelay(null);
			} catch (InsufficientPermissionException ex) {
				Guild g = Main.getJibril().getGuildById(k);
				assert g != null;
				try {
					Objects.requireNonNull(g.getOwner()).getUser()
							.openPrivateChannel()
							.queue(c -> c.sendMessage("""
									❌ | Me faltam permissões para enviar mensagens globais no servidor %s.
																
									Minhas permissões:```
									%s Ler/Enviar mensagens
									%s Inserir links
									%s Anexar arquivos
									%s Ver histórico de mensagens
									%s Usar emojis externos
									%s Gerenciar mensagens
									%s Gerenciar webhooks
									```
									"""
									.formatted(
											g.getName(),
											g.getSelfMember().hasPermission(Permission.MESSAGE_WRITE) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_ATTACH_FILES) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_EXT_EMOJI) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS) ? "✅" : "❌"
									)).queue());
					Helper.logger(this.getClass()).error(ex + " | Sevidor " + g.getName());
				} catch (Exception e) {
					Helper.logger(this.getClass()).error(ex + " | Dono " + Objects.requireNonNull(g.getOwner()).getUser().getAsTag());
				}
			}
		}
		try {
			if (!GuildDAO.getGuildById(s.getId()).isLiteMode()) {
				source.delete().queue();
			}
		} catch (InsufficientPermissionException ignore) {
		}
	}

	public void relayMessage(GlobalMessage gm) {
		updateRelays();
		checkSize();

		EmbedBuilder eb = new EmbedBuilder();

		eb.setDescription(gm.getContent());
		eb.setAuthor(gm.getName(), "https://www.pngkey.com/png/full/334-3346073_no-game-no-life-icon.png", "https://www.pngkey.com/png/full/334-3346073_no-game-no-life-icon.png");
		eb.setThumbnail(RelayBlockList.checkThumb(gm.getUserId()) ? "https://i.pinimg.com/originals/46/15/87/461587d51087bfdf8906149d356f972f.jpg" : gm.getAvatar());
		eb.setFooter(gm.getUserId(), "http://icons.iconarchive.com/icons/killaaaron/adobe-cc-circles/1024/Adobe-Id-icon.png");
		try {
			eb.setColor(Helper.colorThief(gm.getAvatar()));
		} catch (IOException ignore) {
		}

		eb.addField("Enviado via dashboard", "", false);

		MessageBuilder mb = new MessageBuilder();
		mb.setEmbed(eb.build());

		for (Map.Entry<String, String> entry : relays.entrySet()) {
			String k = entry.getKey();
			String r = entry.getValue();
			try {
				TextChannel t = Objects.requireNonNull(Main.getJibril().getGuildById(k)).getTextChannelById(r);
				assert t != null;
				if (GuildDAO.getGuildById(k).isAllowImg()) {
					if (GuildDAO.getGuildById(k).isLiteMode()) {
						WebhookClient client = getClient(t, Main.getJibril().getGuildById(k));
						if (client != null) {
							client.send(getMessage(gm));
							client.close();
						}
					} else {
						t.sendMessage(mb.build()).queue();
					}
				} else {
					if (GuildDAO.getGuildById(k).isLiteMode()) {
						WebhookClient client = getClient(t, Main.getJibril().getGuildById(k));
						if (client != null) {
							client.send(getMessage(gm));
							client.close();
						}
					} else {
						t.sendMessage(mb.build()).queue();
					}
				}
			} catch (NullPointerException e) {
				GuildDAO.getGuildById(k).setCanalRelay(null);
			} catch (InsufficientPermissionException ex) {
				Guild g = Main.getJibril().getGuildById(k);
				assert g != null;
				try {
					Objects.requireNonNull(g.getOwner()).getUser()
							.openPrivateChannel()
							.queue(c -> c.sendMessage("""
									❌ | Me faltam permissões para enviar mensagens globais no servidor %s.
																		
									Permissões que eu possuo:```
									%s Ler/Enviar mensagens
									%s Inserir links
									%s Anexar arquivos
									%s Ver histórico de mensagens
									%s Usar emojis externos
									%s Gerenciar mensagens
									%s Gerenciar webhooks
									```
									"""
									.formatted(
											g.getName(),
											g.getSelfMember().hasPermission(Permission.MESSAGE_WRITE) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_ATTACH_FILES) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_EXT_EMOJI) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) ? "✅" : "❌",
											g.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS) ? "✅" : "❌"
									)).queue());
					Helper.logger(this.getClass()).error(ex + " | Sevidor " + g.getName());
				} catch (Exception e) {
					Helper.logger(this.getClass()).error(ex + " | Dono " + Objects.requireNonNull(g.getOwner()).getUser().getAsTag());
				}
			}
		}
	}

	public MessageEmbed getRelayInfo(GuildConfig gc) {
		updateRelays();
		checkSize();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		eb.setTitle(":globe_with_meridians: Dados do relay");
		eb.addField(":busts_in_silhouette: Clientes conectados: " + relays.size(), "Canal relay: " + (gc.getCanalRelay() == null ? "Não configurado" : Objects.requireNonNull(Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalRelay())).getAsMention()), false);
		eb.addField("Modo:", gc.isLiteMode() ? "Lite" : "Normal", true);
		eb.addField("Imagens:", gc.isAllowImg() ? "Permitidas" : "Negadas", true);

		return eb.build();
	}

	public Map<String, String> getRelayMap() {
		updateRelays();
		checkSize();

		return relays;
	}

	@SuppressWarnings("unchecked")
	private void updateRelays() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM GuildConfig g WHERE canalrelay <> '' AND canalrelay IS NOT NULL", GuildConfig.class);

		List<GuildConfig> gc = q.getResultList();
		gc.removeIf(g -> Main.getJibril().getGuildById(g.getGuildID()) == null);
		for (GuildConfig g : gc) {
			relays.put(g.getGuildID(), g.getCanalRelay());
		}

		em.close();
	}
}
