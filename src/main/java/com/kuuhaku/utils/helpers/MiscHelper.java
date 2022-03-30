/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.utils.helpers;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.WebhookCluster;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.ColorlessWebhookEmbedBuilder;
import com.kuuhaku.model.common.drop.*;
import com.kuuhaku.model.common.interfaces.Prize;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.model.persistent.guild.Buff;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.buttons.Button;
import com.kuuhaku.model.persistent.guild.buttons.ButtonChannel;
import com.kuuhaku.model.persistent.guild.buttons.ButtonMessage;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;

public abstract class MiscHelper {

	private static PrivilegeLevel getPrivilegeLevel(Member member) {
		if (member == null) {
			return PrivilegeLevel.USER;
		} else if (ShiroInfo.NIICHAN.equals(member.getId())) {
			return PrivilegeLevel.NIICHAN;
		} else if (ShiroInfo.getDevelopers().contains(member.getId())) {
			return PrivilegeLevel.DEV;
		} else if (ShiroInfo.getSupports().containsKey(member.getId())) {
			return PrivilegeLevel.SUPPORT;
		} else if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
			return PrivilegeLevel.MOD;
		}

		return PrivilegeLevel.USER;
	}

	public static boolean hasPermission(Member member, PrivilegeLevel privilegeLevel) {
		return getPrivilegeLevel(member).hasAuthority(privilegeLevel);
	}

	public static boolean findMentions(String text) {
		final Pattern everyone = Message.MentionType.EVERYONE.getPattern();
		final Pattern here = Message.MentionType.HERE.getPattern();

		return everyone.matcher(text).matches() || here.matcher(text).matches();
	}

	public static void sendPM(User user, String message) {
		user.openPrivateChannel().queue((channel) -> channel.sendMessage(message).queue(null, MiscHelper::doNothing));
	}

	public static void typeMessage(MessageChannel channel, String message) {
		channel.sendTyping()
				.delay(message.length() * 25 > 10000 ? 10000 : message.length() + 500, TimeUnit.MILLISECONDS)
				.flatMap(s -> channel.sendMessage(makeEmoteFromMention(message)))
				.queue(null, MiscHelper::doNothing);
	}

	public static void typeMessage(MessageChannel channel, String message, Message target) {
		channel.sendTyping()
				.delay(message.length() * 25 > 10000 ? 10000 : message.length() + 500, TimeUnit.MILLISECONDS)
				.flatMap(s -> target.reply(makeEmoteFromMention(message)))
				.queue(null, MiscHelper::doNothing);
	}

	public static void spawnAd(Account acc, MessageChannel channel) {
		if (!acc.hasVoted(false) && MathHelper.chance(1)) {
			channel.sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\nhttps://top.gg/bot/572413282653306901").queue();
		}
	}

	public static Logger logger(Class<?> source) {
		return LogManager.getLogger(source.getName());
	}

	public static Webhook getOrCreateWebhook(TextChannel chn, String name, JDA bot) throws InterruptedException, ExecutionException {
		final Webhook[] webhook = {null};
		List<Webhook> whs = chn.retrieveWebhooks().submit().get();
		whs.stream()
				.filter(w -> Objects.requireNonNull(w.getOwner()).getUser() == bot.getSelfUser())
				.findFirst()
				.ifPresent(w -> webhook[0] = w);

		try {
			if (webhook[0] == null) return Pages.subGet(chn.createWebhook(name));
			else {
				webhook[0].getUrl();
				return webhook[0];
			}
		} catch (NullPointerException e) {
			return Pages.subGet(chn.createWebhook(name));
		}
	}

	public static Webhook getOrCreateWebhook(TextChannel chn, String name) throws InterruptedException, ExecutionException {
		AtomicReference<Webhook> webhook = new AtomicReference<>();
		List<Webhook> whs = chn.retrieveWebhooks().submit().get();
		whs.stream()
				.filter(w -> {
					Member m = w.getOwner();
					return m != null && m.equals(chn.getGuild().getSelfMember());
				})
				.findFirst()
				.ifPresent(webhook::set);

		try {
			if (webhook.get() == null)
				return Pages.subGet(chn.createWebhook(name));
			else {
				return webhook.get();
			}
		} catch (NullPointerException e) {
			return Pages.subGet(chn.createWebhook(name));
		}
	}

	public static String unmention(String text) {
		return text.replace("@everyone", StringHelper.bugText("@everyone")).replace("@here", StringHelper.bugText("@here"));
	}

	public static String makeEmoteFromMention(String text) {
		return unmention(text.replaceAll("\\{(a)?&(\\w+)&(\\d+)}", "<$1:$2:$3>"));
	}

	public static String makeTagFromEmote(String text) {
		return unmention(text.replaceAll("<a?(:\\w+:)\\d+>", "$1"));
	}

	public static void logToChannel(User u, boolean isCommand, PreparedCommand c, String msg, Guild g) {
		GuildConfig gc = GuildDAO.getGuildById(g.getId());
		if (gc == null || gc.getLogChannel() == null) return;

		TextChannel tc = gc.getLogChannel();
		if (tc == null) {
			gc.setLogChannel(null);
			return;
		}

		try {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(StringUtils.abbreviate(msg, 2048));
			if (u != null) eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
			eb.setTimestamp(Instant.now());

			tc.sendMessageEmbeds(eb.build()).queue(null, MiscHelper::doNothing);
		} catch (Exception e) {
			gc.setLogChannel("");
			GuildDAO.updateGuildSettings(gc);
			logger(MiscHelper.class).warn(e + " | " + e.getStackTrace()[0]);
			Member owner = g.getOwner();
			if (owner != null)
				owner.getUser().openPrivateChannel()
						.flatMap(ch -> ch.sendMessage("Canal de log invalidado com o seguinte erro: `%s | %s`".formatted(e.getClass().getSimpleName(), e)))
						.queue(null, MiscHelper::doNothing);
		}
	}

	public static void logToChannel(User u, boolean isCommand, PreparedCommand c, String msg, Guild g, String args) {
		GuildConfig gc = GuildDAO.getGuildById(g.getId());
		if (gc.getLogChannel() == null) return;

		TextChannel tc = gc.getLogChannel();
		if (tc == null) {
			gc.setLogChannel(null);
			return;
		}

		try {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setAuthor("Relatório de log");
			eb.setDescription(StringUtils.abbreviate(msg, 2048));
			eb.addField("Referente:", u.getAsMention(), true);
			if (isCommand) {
				eb.addField("Comando:", gc.getPrefix() + c.getName(), true);
				eb.addField("Argumentos:", StringUtils.abbreviate(args, 1024), true);
			}
			eb.setTimestamp(Instant.now());

			tc.sendMessageEmbeds(eb.build()).queue(null, MiscHelper::doNothing);
		} catch (Exception e) {
			gc.setLogChannel("");
			GuildDAO.updateGuildSettings(gc);
			logger(MiscHelper.class).warn(e + " | " + e.getStackTrace()[0]);
			Member owner = g.getOwner();
			if (owner != null)
				owner.getUser().openPrivateChannel()
						.flatMap(ch -> ch.sendMessage("Canal de log invalidado com o seguinte erro: `%s | %s`".formatted(e.getClass().getSimpleName(), e)))
						.queue(null, MiscHelper::doNothing);
		}
	}

	public static boolean hasPermission(Member m, Permission p, TextChannel c) {
		return m.getPermissions(c).contains(p);
	}

	public static String getCurrentPerms(TextChannel c) {
		Member shiro = c.getGuild().getSelfMember();
		EnumSet<Permission> perms = shiro.getPermissionsExplicit(c);

		return "__**Permissões atuais da Shiro**__\n\n" +
				perms.stream()
						.map(p -> "✅ -> " + p.getName())
						.sorted()
						.collect(Collectors.joining("\n"));
	}

	public static void finishEmbed(Guild guild, List<Page> pages, List<MessageEmbed.Field> f, EmbedBuilder eb, int i) {
		eb.setColor(ImageHelper.getRandomColor());
		eb.setAuthor("Para usar estes emotes, utilize o comando \"" + GuildDAO.getGuildById(guild.getId()).getPrefix() + "say MENÇÃO\"");
		eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (Math.min(10 * (i + 1), f.size())) + " resultados.", null);

		pages.add(new InteractPage(eb.build()));
	}

	public static void refreshButtons(GuildConfig gc) {
		Set<ButtonChannel> channels = gc.getButtonConfigs();
		if (channels.isEmpty()) return;

		Guild g = Main.getGuildByID(gc.getGuildId());
		if (g != null) {
			for (ButtonChannel channel : channels) {
				TextChannel chn = g.getTextChannelById(channel.getId());

				if (chn == null) {
					gc.getButtonConfigs().remove(channel);
					GuildDAO.updateGuildSettings(gc);
				} else {
					for (ButtonMessage message : channel.getMessages()) {
						Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
						Message msg;
						try {
							msg = chn.retrieveMessageById(message.getId()).submit().get();
						} catch (MissingAccessException | ExecutionException | InterruptedException e) {
							GuildConfig conf = GuildDAO.getGuildById(g.getId());
							for (ButtonChannel bc : conf.getButtonConfigs()) {
								if (bc.removeMessage(message)) break;
							}

							GuildDAO.updateGuildSettings(gc);
							continue;
						}
						resolveButton(g, message.getButtons(), buttons);

						if (MiscHelper.hasPermission(g.getSelfMember(), MESSAGE_MANAGE, chn))
							msg.clearReactions().queue();

						if (message.isGatekeeper()) {
							Role r = message.getRole(g);

							gatekeep(msg, r);
						} else {
							buttons.put(StringHelper.parseEmoji(Constants.CANCEL), wrapper -> {
								if (wrapper.getUser().getId().equals(message.getAuthor())) {
									GuildConfig conf = GuildDAO.getGuildById(g.getId());
									for (ButtonChannel bc : conf.getButtonConfigs()) {
										if (bc.removeMessage(message)) break;
									}

									GuildDAO.updateGuildSettings(conf);
									wrapper.getMessage().clearReactions().queue();
								}
							});

							Pages.buttonize(msg, buttons, Constants.USE_BUTTONS, true);
						}
					}
				}
			}
		}
	}

	public static void resolveButton(Guild g, List<Button> jo, Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons) {
		for (Button b : jo) {
			Role role = b.getRole(g);

			buttons.put(StringHelper.parseEmoji(b.getEmote()), wrapper -> {
				if (role != null) {
					try {
						Member m = wrapper.getMember();
						if (m.getRoles().contains(role)) {
							g.removeRoleFromMember(m, role).queue(null, MiscHelper::doNothing);
						} else {
							g.addRoleToMember(wrapper.getMember(), role).queue(null, MiscHelper::doNothing);
						}
					} catch (InsufficientPermissionException | HierarchyException ignore) {
					}
				} else {
					wrapper.getMessage().clearReactions().queue(s -> {
						wrapper.getChannel().sendMessage(":warning: | Botão removido devido a cargo inexistente.").queue();
						GuildConfig gc = GuildDAO.getGuildById(g.getId());

						b.getParent().removeButton(b);

						GuildDAO.updateGuildSettings(gc);
					});
				}
			});
		}
	}

	public static void gatekeep(Message m, Role r) {
		if (m == null) return;

		Pages.buttonize(m, new LinkedHashMap<>() {{
			put(StringHelper.parseEmoji("☑"), wrapper -> {
				try {
					wrapper.getMessage().getGuild().addRoleToMember(wrapper.getMember(), r).queue();
				} catch (InsufficientPermissionException | HierarchyException ignore) {
				}
			});
			put(StringHelper.parseEmoji("\uD83D\uDEAA"), wrapper -> {
				try {
					wrapper.getMember().kick("Não aceitou as regras.").queue();
				} catch (InsufficientPermissionException | HierarchyException ignore) {
				}
			});
		}}, Constants.USE_BUTTONS, false);
	}

	public static void addButton(String[] args, Message message, MessageChannel channel, GuildConfig gc, String s2, boolean gatekeeper) {
		Role r = message.getMentionedRoles().get(0);

		Set<ButtonChannel> channels = gc.getButtonConfigs();
		ButtonChannel bc = channels.stream()
				.filter(chn -> chn.getId().equals(channel.getId()))
				.findFirst()
				.orElse(null);

		if (bc == null) {
			bc = new ButtonChannel(channel.getId());
			gc.getButtonConfigs().add(bc);
		}

		Set<ButtonMessage> messages = bc.getMessages();
		ButtonMessage bm = messages.stream()
				.filter(msg -> msg.getId().equals(args[0]))
				.findFirst()
				.orElse(null);

		if (bm == null) {
			bm = new ButtonMessage(
					args[0],
					message.getAuthor().getId(),
					gatekeeper,
					gatekeeper ? r.getId() : null
			);

			if (gatekeeper && bc.getMessages().stream().anyMatch(ButtonMessage::isGatekeeper)) {
				Set<ButtonMessage> msgs = Set.copyOf(bc.getMessages());
				for (ButtonMessage bMsg : msgs) {
					if (bMsg.isGatekeeper()) {
						bc.removeMessage(bMsg);
					}
				}
			}

			bc.addMessage(bm);
		}

		if (!gatekeeper) {
			String id;
			if (StringHelper.parseEmoji(s2).isUnicode()) {
				id = s2;
			} else {
				Emote e = Main.getShiro().getEmoteById(s2);
				if (e == null) throw new IllegalArgumentException();
				else id = e.getId();
			}

			bm.addButton(new Button(r.getId(), id));
		}

		GuildDAO.updateGuildSettings(gc);
	}

	public static InviteAction createInvite(Guild guild) {
		TextChannel def = guild.getDefaultChannel();
		if (def != null && guild.getSelfMember().hasPermission(def, Permission.CREATE_INSTANT_INVITE)) {
			return def.createInvite();
		}

		for (TextChannel tc : guild.getTextChannels()) {
			if (guild.getSelfMember().hasPermission(tc, Permission.CREATE_INSTANT_INVITE))
				return tc.createInvite();
		}

		return null;
	}

	public static String replaceEmotes(String msg) {
		String[] args = msg.split(" ");

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(":") && args[i].endsWith(":")) {
				Emote e = Main.getShiro().getEmoteById(Main.getEmoteCache().getOrDefault(args[i], "0"));
				if (e != null) {
					args[i] = e.getAsMention();
				}
			}
		}

		return String.join(" ", args);
	}

	public static String sendEmotifiedString(Guild g, String text) {
		for (Emote e : g.getEmotes()) {
			if (e.getName().startsWith("TEMP_")) {
				try {
					e.delete().queue(null, MiscHelper::doNothing);
				} catch (ErrorResponseException ignore) {
				}
			}
		}

		text = makeEmoteFromMention(text);
		text = makeTagFromEmote(text);

		String[] lines = text.split("\n");
		for (int l = 0; l < lines.length; l++) {
			String[] words = lines[l].split(" ");
			for (int i = 0, emotes = 0, slots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(e -> !e.isAnimated()).count(), aSlots = g.getMaxEmotes() - (int) g.getEmotes().stream().filter(Emote::isAnimated).count(); i < words.length && emotes < 10; i++) {
				String word = words[i];
				if (!word.matches(":.+:")) {
					words[i] = word;
					continue;
				}

				String id = Main.getEmoteCache().getOrDefault(word, "0");
				Emote e = id == null ? null : Main.getShiro().getEmoteById(id);

				if (e != null) {
					try {
						boolean animated = e.isAnimated();
						if ((animated ? aSlots : slots) > 0) {
							e = Pages.subGet(g.createEmote(
									"TEMP_" + e.getName(),
									Icon.from(ImageHelper.getImage(e.getImageUrl())),
									g.getSelfMember().getRoles().get(0)
							));

							if (animated) aSlots--;
							else slots--;
						}

						words[i] = e.getAsMention();
						emotes++;
					} catch (IOException ex) {
						logger(MiscHelper.class).error(ex + " | " + ex.getStackTrace()[0]);
					}
				}
			}

			lines[l] = String.join(" ", words);
		}

		return String.join("\n", lines);
	}

	public static void doNothing(Throwable t) {
		try {
			logger(MiscHelper.class).debug(t + " | " + t.getStackTrace()[0]);
			throw t;
		} catch (Throwable ignore) {
		}
	}

	public static String generateToken(String seed, int length) {
		byte[] nameSpace = seed.getBytes(StandardCharsets.UTF_8);
		byte[] randomSpace = new byte[length];
		Constants.DEFAULT_SECURE_RNG.nextBytes(randomSpace);

		return ImageHelper.atob(nameSpace) + "." + ImageHelper.atob(randomSpace);
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act) {
		Main.getEvents().addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(u.getId())) {
					if (act.apply(event.getMessage())) close();
				}
			}
		});
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act, int time, TimeUnit unit) {
		Main.getEvents().addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			final ScheduledFuture<?> timeout = Executors.newSingleThreadScheduledExecutor().schedule(this::close, time, unit);

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(u.getId())) {
					if (act.apply(event.getMessage())) {
						timeout.cancel(true);
						close();
					}
				}
			}
		});
	}

	public static void awaitMessage(User u, TextChannel chn, Function<Message, Boolean> act, int time, TimeUnit unit, Runnable onTimeout) {
		Main.getEvents().addHandler(chn.getGuild(), new SimpleMessageListener(chn) {
			final ScheduledFuture<?> timeout = Executors.newSingleThreadScheduledExecutor().schedule(() -> {
				close();
				onTimeout.run();
			}, time, unit);

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(u.getId())) {
					if (act.apply(event.getMessage())) {
						timeout.cancel(true);
						close();
					}
				}
			}
		});
	}

	public static String replaceWith(String source, Map<String, String> replaces) {
		AtomicReference<String> toChange = new AtomicReference<>();
		for (Map.Entry<String, String> entry : replaces.entrySet()) {
			String k = entry.getKey();
			String v = entry.getValue();
			toChange.set(source.replace(k, v));
		}
		return toChange.get();
	}

	public static void spawnKawaipon(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getRatelimit().containsKey("kawaipon_" + gc.getGuildId())) return;

		double cardBuff = getBuffMult(gc, BuffType.CARD);
		double foilBuff = getBuffMult(gc, BuffType.FOIL);

		if (MathHelper.chance((3 - MathHelper.clamp(channel.getGuild().getMemberCount() / 5000, 0, 1)) * cardBuff)) {
			Main.getInfo().getRatelimit().put("kawaipon_" + gc.getGuildId(), true, 1, TimeUnit.MINUTES);

			List<org.apache.commons.math3.util.Pair<KawaiponRarity, Double>> odds = new ArrayList<>();
			for (KawaiponRarity kr : KawaiponRarity.validValues()) {
				odds.add(org.apache.commons.math3.util.Pair.create(kr, Math.pow(2, 5 - kr.getIndex())));
			}

			KawaiponRarity kr = MathHelper.getRandom(odds);

			List<Card> cards = Card.getCards(kr);
			Card c = CollectionHelper.getRandomEntry(cards);
			boolean foil = MathHelper.chance(0.5 * foilBuff);
			KawaiponCard kc = new KawaiponCard(c, foil);
			BufferedImage img = c.drawCard(foil);

			EmbedBuilder eb = new EmbedBuilder()
					.setAuthor("Uma carta " + c.getRarity().toString().toUpperCase(Locale.ROOT) + " Kawaipon apareceu neste servidor!")
					.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
					.setColor(ImageHelper.colorThief(img))
					.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + StringHelper.separate(c.getRarity().getIndex() * Constants.BASE_CARD_PRICE * (foil ? 2 : 1)) + " CR).", null);

			if (gc.isSmallCards())
				eb.setThumbnail("attachment://kawaipon.png");
			else
				eb.setImage("attachment://kawaipon.png");

			try {
				if (gc.getKawaiponChannel() == null) {
					channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(img, "kp_" + c.getId(), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, MiscHelper::doNothing);
				} else {
					TextChannel tc = gc.getKawaiponChannel();

					if (tc == null) {
						gc.setKawaiponChannel(null);
						GuildDAO.updateGuildSettings(gc);
						channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, MiscHelper::doNothing);
					} else {
						tc.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, MiscHelper::doNothing);
					}
				}

				Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
			} catch (IllegalStateException ignore) {
			}
		}
	}

	public static void forceSpawnKawaipon(GuildConfig gc, TextChannel channel, User user, AddedAnime anime, boolean foil) {
		double foilBuff = getBuffMult(gc, BuffType.FOIL);
		List<Card> cards;

		if (anime != null) {
			List<Card> cds = Card.getCards(anime.getName());
			Set<KawaiponRarity> rarities = cds.stream()
					.map(Card::getRarity)
					.collect(Collectors.toSet());

			List<org.apache.commons.math3.util.Pair<KawaiponRarity, Double>> odds = new ArrayList<>();
			for (KawaiponRarity kr : KawaiponRarity.validValues()) {
				if (!rarities.contains(kr)) continue;

				odds.add(org.apache.commons.math3.util.Pair.create(kr, Math.pow(2, 5 - kr.getIndex())));
			}

			KawaiponRarity kr = MathHelper.getRandom(odds);

			cards = cds.stream().filter(c -> c.getRarity() == kr).collect(Collectors.toList());
		} else {
			List<org.apache.commons.math3.util.Pair<KawaiponRarity, Double>> odds = new ArrayList<>();
			for (KawaiponRarity kr : KawaiponRarity.validValues()) {
				odds.add(org.apache.commons.math3.util.Pair.create(kr, Math.pow(2, 5 - kr.getIndex())));
			}

			cards = Card.getCards(MathHelper.getRandom(odds));
		}

		Card c = CollectionHelper.getRandomEntry(cards);
		foil = foil || MathHelper.chance(0.5 * foilBuff);
		KawaiponCard kc = new KawaiponCard(c, foil);
		BufferedImage img = c.drawCard(foil);

		EmbedBuilder eb = new EmbedBuilder()
				.setAuthor(user.getName() + " invocou uma carta " + c.getRarity().toString().toUpperCase(Locale.ROOT) + " neste servidor!")
				.setTitle(kc.getName() + " (" + c.getAnime().toString() + ")")
				.setColor(ImageHelper.colorThief(img))
				.setFooter("Digite `" + gc.getPrefix() + "coletar` para adquirir esta carta (necessário: " + StringHelper.separate(c.getRarity().getIndex() * Constants.BASE_CARD_PRICE * (foil ? 2 : 1)) + " CR).", null);

		if (gc.isSmallCards())
			eb.setThumbnail("attachment://kawaipon.png");
		else
			eb.setImage("attachment://kawaipon.png");

		try {
			if (gc.getKawaiponChannel() == null) {
				channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(img, "kp_" + c.getId(), "png"), "kawaipon.png")
						.delay(1, TimeUnit.MINUTES)
						.flatMap(Message::delete)
						.queue(null, MiscHelper::doNothing);
			} else {
				TextChannel tc = gc.getKawaiponChannel();

				if (tc == null) {
					gc.setKawaiponChannel(null);
					GuildDAO.updateGuildSettings(gc);
					channel.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, MiscHelper::doNothing);
				} else {
					tc.sendMessageEmbeds(eb.build()).addFile(ImageHelper.writeAndGet(c.drawCard(foil), "kp_" + c.getId() + (foil ? "_f" : ""), "png"), "kawaipon.png")
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, MiscHelper::doNothing);
				}
			}

			Main.getInfo().getCurrentCard().put(channel.getGuild().getId(), kc);
		} catch (IllegalStateException ignore) {
		}
	}

	public static void spawnDrop(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getRatelimit().containsKey("drop_" + gc.getGuildId())) return;

		double dropBuff = getBuffMult(gc, BuffType.DROP);

		if (MathHelper.chance((2.5 - MathHelper.clamp(channel.getGuild().getMemberCount() * 0.75f / 5000, 0, 0.75)) * dropBuff)) {
			Main.getInfo().getRatelimit().put("drop_" + gc.getGuildId(), true, 1, TimeUnit.MINUTES);

			Prize<?> drop;
			int type = MathHelper.rng(1000);

			if (type >= 995)
				drop = new FieldDrop();
			else if (type >= 975)
				drop = new EvogearDrop();
			else if (type >= 900)
				drop = new ChampionDrop();
			else
				drop = new CreditDrop();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setThumbnail("https://i.pinimg.com/originals/86/c0/f4/86c0f4d0f020c3f819a532873ef33704.png")
					.setTitle("Um drop apareceu neste servidor!")
					.addField("Conteúdo:", drop.toString(), true)
					.addField("Código captcha:", drop.getCaptcha(), true)
					.setFooter("Digite `" + gc.getPrefix() + "abrir` para receber o prêmio (requisito: " + drop.getRequirement().getKey() + ").", null);

			try {
				if (gc.getDropChannel() == null) {
					channel.sendMessageEmbeds(eb.build())
							.delay(1, TimeUnit.MINUTES)
							.flatMap(Message::delete)
							.queue(null, MiscHelper::doNothing);
				} else {
					TextChannel tc = gc.getDropChannel();

					if (tc == null) {
						gc.setDropChannel(null);
						GuildDAO.updateGuildSettings(gc);
						channel.sendMessageEmbeds(eb.build())
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, MiscHelper::doNothing);
					} else {
						tc.sendMessageEmbeds(eb.build())
								.delay(1, TimeUnit.MINUTES)
								.flatMap(Message::delete)
								.queue(null, MiscHelper::doNothing);
					}
				}

				Main.getInfo().getCurrentDrop().put(channel.getGuild().getId(), drop);
			} catch (IllegalStateException ignore) {
			}
		}
	}

	public static void spawnPadoru(GuildConfig gc, TextChannel channel) {
		String padoru = Constants.RESOURCES_URL + "/assets/padoru_padoru.gif";
		if (Main.getInfo().getSpecialEvent().containsKey(gc.getGuildId())) return;

		if (MathHelper.chance(0.1 - MathHelper.clamp(channel.getGuild().getMemberCount() * 0.08 / 5000, 0, 0.08))) {
			Main.getInfo().getSpecialEvent().put(gc.getGuildId(), true);

			try {
				TextChannel tc = CollectionHelper.getOr(gc.getDropChannel(), channel);
				Webhook wh = getOrCreateWebhook(tc, "Shiro");
				WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();

				WebhookMessageBuilder wmb = new WebhookMessageBuilder();
				wmb.setUsername("Nero (Evento Padoru)");
				wmb.setAvatarUrl(Constants.NERO_AVATAR.formatted(1));

				List<Prize<?>> prizes = new ArrayList<>();
				for (int i = 0; i < 6; i++) {
					int type = MathHelper.rng(1000);

					if (type >= 997 && prizes.stream().noneMatch(p -> p.getClass() == PadoruDrop.class))
						prizes.add(new PadoruDrop());
					else if (type >= 995)
						prizes.add(new FieldDrop());
					else if (type >= 975)
						prizes.add(new EvogearDrop());
					else if (type >= 900)
						prizes.add(new ChampionDrop());
					else
						prizes.add(new CreditDrop());
				}

				ColorlessWebhookEmbedBuilder web = new ColorlessWebhookEmbedBuilder()
						.setDescription("""
								**Hashire sori yo**
								**Kaze no you ni**
								**Tsukimihara wo...**
								""")
						.setThumbnailUrl(Constants.RESOURCES_URL + "/assets/padoru.gif")
						.setTitle("Nero Claudius apareceu trazendo presentes neste servidor!");

				for (int i = 0; i < prizes.size(); i++) {
					Prize<?> prize = prizes.get(i);
					web.addField("Presente " + (i + 1) + ":", prize.toString(), true);
				}

				web.setFooter("Complete a música para participar do sorteio dos prêmios.", null);

				Set<String> users = new HashSet<>();
				SimpleMessageListener sml = new SimpleMessageListener(channel) {
					@Override
					public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
						String msg = event.getMessage().getContentRaw();
						User author = event.getAuthor();
						if (msg.equalsIgnoreCase("PADORU PADORU") && !author.isBot() && users.add(author.getId())) {
							Emote e = Main.getShiro().getEmoteById("787012642501689344");
							if (e != null) event.getMessage().addReaction(e).queue();
						}
					}
				};

				Main.getEvents().addHandler(channel.getGuild(), sml);
				Consumer<Message> act = msg -> {
					if (users.size() > 0) {
						List<String> ids = List.copyOf(users);
						User u = Main.getUserByID(CollectionHelper.getRandomEntry(ids));

						ColorlessWebhookEmbedBuilder neb = new ColorlessWebhookEmbedBuilder()
								.setImageUrl(padoru);

						for (int i = 0; i < prizes.size(); i++) {
							Prize<?> prize = prizes.get(i);
							neb.addField("Presente " + (i + 1) + ":", prize.toString(u), true);
							prize.award(u);
						}

						wc.send(wmb.resetEmbeds()
								.setContent("Decidi que " + u.getAsMention() + " merece os presentes!")
								.addEmbeds(neb.build())
								.build());
					} else {
						wc.send(wmb.resetEmbeds()
								.setContent("Decidi que ninguém merece os presentes!")
								.build());
					}

					wc.close();
					sml.close();
				};

				ReadonlyMessage rm = wc.send(wmb.addEmbeds(web.build()).build()).get();
				if (gc.getDropChannel() == null) {
					tc.retrieveMessageById(rm.getId())
							.delay(1, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, MiscHelper::doNothing);
								act.accept(msg);
							});
				} else {
					tc.retrieveMessageById(rm.getId())
							.delay(1, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, MiscHelper::doNothing);
								act.accept(msg);
							});
				}
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			}
		}
	}

	public static void spawnUsaTan(GuildConfig gc, TextChannel channel) {
		if (Main.getInfo().getSpecialEvent().containsKey(gc.getGuildId())) return;

		if (MathHelper.chance(0.15 - MathHelper.clamp(channel.getGuild().getMemberCount() * 0.5 / 5000, 0, 0.05))) {
			Main.getInfo().getSpecialEvent().put(gc.getGuildId(), true);

			try {
				TextChannel tc = CollectionHelper.getOr(gc.getDropChannel(), channel);
				Webhook wh = getOrCreateWebhook(tc, "Shiro");
				WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();

				WebhookMessageBuilder wmb = new WebhookMessageBuilder();
				wmb.setUsername("Usa-tan (Evento Páscoa)");
				wmb.setAvatarUrl(Constants.USATAN_AVATAR.formatted(1));

				Emote egg = Main.getShiro().getEmoteById(TagIcons.EASTER_EGG.getId(0));
				assert egg != null;

				List<Prize<?>> prizes = new ArrayList<>();
				for (int i = 0; i < 6; i++) {
					int type = MathHelper.rng(1000);

					if (type >= 995)
						prizes.add(new FieldDrop());
					else if (type >= 975)
						prizes.add(new EvogearDrop());
					else if (type >= 900)
						prizes.add(new ChampionDrop());
					else
						prizes.add(new CreditDrop());
				}

				ColorlessWebhookEmbedBuilder web = new ColorlessWebhookEmbedBuilder()
						.setDescription("Hehe, será que você consegue achar o ovo de páscoa que eu escondi neste canal?")
						.setThumbnailUrl(egg.getImageUrl())
						.setTitle("Usa-tan apareceu trazendo presentes neste servidor!");

				for (int i = 0; i < prizes.size(); i++) {
					Prize<?> prize = prizes.get(i);
					web.addField("Ovo " + (i + 1) + ":", prize.toString(), true);
				}

				web.setFooter("Enconte a reação de ovo de páscoa escondido em uma das mensagens neste canal.", null);

				AtomicBoolean found = new AtomicBoolean(false);
				AtomicBoolean finished = new AtomicBoolean(false);
				Runnable act = () -> {
					if (finished.get()) return;
					wc.send(wmb.resetEmbeds()
							.setContent("Ninguem encontrou o ovo de páscoa a tempo!")
							.build());
					wc.close();
					finished.set(true);
				};

				List<Message> hist = Pages.subGet(tc.getHistory().retrievePast(100));

				if (hist.size() == 0) return;

				Message m = CollectionHelper.getRandomEntry(hist);
				Pages.buttonize(m, Collections.singletonMap(
						StringHelper.parseEmoji(egg.getId()), wrapper -> {
							if (finished.get()) return;

							ColorlessWebhookEmbedBuilder neb = new ColorlessWebhookEmbedBuilder();
							for (int i = 0; i < prizes.size(); i++) {
								Prize<?> prize = prizes.get(i);
								neb.addField("Ovo " + (i + 1) + ":", prize.toString(wrapper.getUser()), true);
								prize.award(wrapper.getUser());
							}

							wc.send(wmb.resetEmbeds()
									.setContent(wrapper.getUser().getAsMention() + " encontrou o ovo de páscoa!")
									.addEmbeds(neb.build())
									.build());
							wc.close();
							found.set(true);
							finished.set(true);
						}
				), Constants.USE_BUTTONS, false, 2, TimeUnit.MINUTES);

				ReadonlyMessage rm = wc.send(wmb.addEmbeds(web.build()).build()).get();
				if (gc.getDropChannel() == null) {
					tc.retrieveMessageById(rm.getId())
							.delay(2, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, MiscHelper::doNothing);
								if (!found.get()) act.run();
							}, MiscHelper::doNothing);
				} else {
					tc.retrieveMessageById(rm.getId())
							.delay(2, TimeUnit.MINUTES)
							.queue(msg -> {
								msg.delete().queue(null, MiscHelper::doNothing);
								if (!found.get()) act.run();
							}, MiscHelper::doNothing);
				}
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException ignore) {
			}
		}
	}

	public static CardStatus checkStatus(Kawaipon kp) {
		int normalCount = kp.getNormalCards().size();
		int foilCount = kp.getFoilCards().size();
		int total = Card.queryNative(Number.class, "SELECT COUNT(1) FROM Card").intValue();

		if (normalCount + foilCount == total * 2) return CardStatus.NO_CARDS;
		else if (foilCount == total) return CardStatus.NORMAL_CARDS;
		else if (normalCount == total) return CardStatus.FOIL_CARDS;
		else return CardStatus.ALL_CARDS;
	}

	public static Deck getDailyDeck() {
		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));
		long seed = Long.parseLong("" + today.getYear() + today.getMonthValue() + today.getDayOfMonth());
		Deck dk = new Deck();

		dk.setChampions(CollectionHelper.getRandomN(Champion.getChampions(false), 30, 3, seed));
		dk.setEquipments(CollectionHelper.getRandomN(Evogear.getEvogears(false), 6, 3, seed));
		dk.setFields(CollectionHelper.getRandomN(Field.getFields(false), 1, 3, seed));

		return dk;
	}

	public static void broadcast(String message, TextChannel channel) {
		List<WebhookClient> clients = new ArrayList<>();
		List<GuildConfig> gcs = GuildDAO.getAlertChannels();

		int success = 0;
		int failed = 0;

		for (GuildConfig gc : gcs) {
			Guild g = Main.getGuildByID(gc.getGuildId());
			if (g == null) continue;
			try {
				TextChannel c = gc.getAlertChannel();
				if (c != null && c.canTalk()) {
					Webhook wh = getOrCreateWebhook(c, "Notificações Shiro");
					if (wh == null) failed++;

					else {
						WebhookClientBuilder wcb = new WebhookClientBuilder(wh.getUrl());
						clients.add(wcb.build());
						success++;
					}
				} else failed++;
			} catch (Exception e) {
				failed++;
			}
		}

		WebhookMessageBuilder wmb = new WebhookMessageBuilder();
		wmb.setUsername("Stephanie (Notificações Shiro)");

		int v;
		if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER)
			v = 2;
		else
			v = 1;

		wmb.setAvatarUrl(Constants.STEPHANIE_AVATAR.formatted(v));
		wmb.setContent(message);
		WebhookCluster cluster = new WebhookCluster(clients);
		cluster.broadcast(wmb.build());
		if (channel != null)
			channel.sendMessage(":loud_sound: | Sucesso: " + success + "\n:mute: | Falha: " + failed).queue();
	}

	public static String replaceTags(String text, User user, Guild guild, Message msg) {
		Map<String, String> reps = new HashMap<>() {{
			if (user != null) {
				put("%user%", user.getAsMention());
				put("%user.id%", user.getId());
				put("%user.name%", user.getName());
				put("%user.created%", Constants.TIMESTAMP.formatted(user.getTimeCreated().toEpochSecond()));

				int raids = RaidInfo.queryNative(Number.class, "SELECT COUNT(1) FROM RaidMember r WHERE r.uid = :uid", user.getId()).intValue();
				put("%user.raids%", StringHelper.separate(raids));
			}

			if (guild != null) {
				put("%guild%", guild.getName());
				put("%guild.count%", StringHelper.separate(guild.getMemberCount()));
			}

			if (msg != null) {
				put("%message%", msg.getContentRaw());
			}
		}};

		for (Map.Entry<String, String> rep : reps.entrySet()) {
			text = text.replace(rep.getKey(), rep.getValue());
		}

		return makeEmoteFromMention(text);
	}

	public static boolean isPinging(Message msg, String id) {
		User u = Main.getUserByID(id);
		return msg.isMentioned(u, Message.MentionType.USER);
	}

	public static <T> MessageAction generateStore(User u, TextChannel chn, String title, String desc, Color color, List<T> items, Function<T, MessageEmbed.Field> fieldExtractor) {
		Account acc = Account.find(Account.class, u.getId());
		EmbedBuilder eb = new EmbedBuilder()
				.setTitle(title)
				.setDescription(desc)
				.setColor(color)
				.setFooter("""
						💰 CR: %s
						♦️ Gemas: %s
						""".formatted(StringHelper.separate(acc.getBalance()), StringHelper.separate(acc.getGems())));

		for (T item : items) {
			eb.addField(fieldExtractor.apply(item));
		}

		return chn.sendMessageEmbeds(eb.build());
	}

	public static boolean findParam(String[] args, String... param) {
		return Arrays.stream(args).anyMatch(s -> LogicHelper.equalsAny(s, param));
	}

	public static XYChart buildXYChart(String title, Pair<String, String> axis, List<Color> colors) {
		XYChart chart = new XYChartBuilder()
				.width(1920)
				.height(1080)
				.title(title)
				.xAxisTitle(axis.getLeft())
				.yAxisTitle(axis.getRight())
				.build();

		chart.getStyler()
				.setPlotGridLinesColor(new Color(64, 68, 71))
				.setAxisTickLabelsColor(Color.WHITE)
				.setAnnotationsFontColor(Color.WHITE)
				.setChartFontColor(Color.WHITE)
				.setHasAnnotations(true)
				.setLegendPosition(Styler.LegendPosition.InsideNE)
				.setSeriesColors(colors.toArray(Color[]::new))
				.setPlotBackgroundColor(new Color(32, 34, 37))
				.setChartBackgroundColor(new Color(16, 17, 20))
				.setLegendBackgroundColor(new Color(16, 17, 20, 100))
				.setSeriesLines(Collections.nCopies(6, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)).toArray(BasicStroke[]::new));

		return chart;
	}

	public static CategoryChart buildBarChart(String title, Pair<String, String> axis, List<Color> colors) {
		CategoryChart chart = new CategoryChartBuilder()
				.width(1920)
				.height(1080)
				.title(title)
				.xAxisTitle(axis.getLeft())
				.yAxisTitle(axis.getRight())
				.build();

		chart.getStyler()
				.setPlotGridLinesColor(new Color(64, 68, 71))
				.setAxisTickLabelsColor(Color.WHITE)
				.setAnnotationsFontColor(Color.WHITE)
				.setChartFontColor(Color.WHITE)
				.setHasAnnotations(true)
				.setLegendPosition(Styler.LegendPosition.InsideNE)
				.setSeriesColors(colors.toArray(Color[]::new))
				.setPlotBackgroundColor(new Color(32, 34, 37))
				.setChartBackgroundColor(new Color(16, 17, 20))
				.setLegendBackgroundColor(new Color(16, 17, 20, 100));

		return chart;
	}

	public static int roundToBit(int value) {
		return 1 << (int) Math.round(MathHelper.log(value, 2));
	}

	public static int roundTrunc(int value, int mult) {
		return mult * Math.round((float) value / mult);
	}

	public static long roundTrunc(long value, int mult) {
		return mult * Math.round((double) value / mult);
	}

	public static int roundTrunc(float value, int mult) {
		return mult * Math.round(value / mult);
	}

	public static long roundTrunc(double value, int mult) {
		return mult * Math.round(value / mult);
	}

	public static String getImageFrom(Message m) {
		if (!m.getAttachments().isEmpty()) {
			Message.Attachment att = m.getAttachments().get(0);
			if (att.isImage())
				return att.getUrl();
		} else if (!m.getEmbeds().isEmpty()) {
			MessageEmbed emb = m.getEmbeds().get(0);
			if (emb.getImage() != null)
				return emb.getImage().getProxyUrl();
		} else if (!m.getEmotes().isEmpty()) {
			Emote e = m.getEmotes().stream().findFirst().orElse(null);
			if (e != null)
				return m.getEmotes().get(0).getImageUrl();
		}

		return null;
	}

	public static String getUsername(String id) {
		User u = Main.getUserByID(id);
		return u == null ? UsernameDAO.getUsername(id) : u.getName();
	}

	public static double getBuffMult(GuildConfig gc, BuffType type) {
		return gc.getBuffs().stream()
				.filter(b -> b.getType() == type)
				.mapToDouble(Buff::getMultiplier)
				.findFirst().orElse(1);
	}
}
