/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.listener;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.InvalidSignatureException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.model.common.*;
import com.kuuhaku.model.common.drop.Drop;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.*;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.model.persistent.user.*;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.model.records.PreparedCommand;
import com.kuuhaku.util.*;
import com.kuuhaku.util.json.JSONObject;
import me.xuender.unidecode.Unidecode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GuildListener extends ListenerAdapter {
	private static final ExpiringMap<String, Boolean> ratelimit = ExpiringMap.builder().variableExpiration().build();
	private static final ConcurrentMap<String, ExpiringMap<String, Message>> messages = new ConcurrentHashMap<>();
	private static final Map<String, CopyOnWriteArrayList<SimpleMessageListener>> toHandle = new ConcurrentHashMap<>();

	@Override
	public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
		if (event.getUser().isBot()) return;

		MessageReaction.ReactionEmote reaction = event.getReactionEmote();
		if (reaction.isEmoji() && reaction.getEmoji().equals("\u2b50")) {
			GuildConfig config = DAO.find(GuildConfig.class, event.getGuild().getId());
			Message msg = Pages.subGet(event.getChannel().retrieveMessageById(event.getMessageId()));

			TextChannel channel = config.getSettings().getStarboardChannel();
			if (channel == null) return;

			int stars = (int) msg.getReactions().stream()
					.filter(r -> r.getReactionEmote().isEmoji() && r.getReactionEmote().getEmoji().equals("\u2b50"))
					.flatMap(r -> r.retrieveUsers().stream())
					.filter(u -> !u.isBot() && !u.equals(msg.getAuthor()))
					.count();

			if (stars >= config.getSettings().getStarboardThreshold() && DAO.find(StarredMessage.class, msg.getId()) == null) {
				new StarredMessage(msg.getId()).save();

				Message.Attachment img = null;
				for (Message.Attachment att : msg.getAttachments()) {
					if (att.isImage()) {
						img = att;
						break;
					}
				}

				Message ref = msg.getReferencedMessage();
				Member author = msg.getMember();
				assert author != null;

				EmbedBuilder eb = new EmbedBuilder()
						.setColor(Color.ORANGE)
						.setTitle(config.getLocale().get("str/highlight").formatted(author.getEffectiveName()), msg.getJumpUrl())
						.setDescription(StringUtils.abbreviate(msg.getContentRaw(), MessageEmbed.DESCRIPTION_MAX_LENGTH));

				if (ref != null) {
					eb.setAuthor(
							StringUtils.abbreviate(ref.getContentRaw(), 128),
							ref.getJumpUrl(),
							"https://getmeroof.com/gmr-assets/reply.png"
					);
				}
				if (img != null) {
					eb.setImage(img.getUrl());
				}

				channel.sendMessage(":star: | " + event.getChannel().getAsMention()).setEmbeds(eb.build()).queue();
			}
		}
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		if (event.getUser().isBot()) return;

		GuildConfig config = DAO.find(GuildConfig.class, event.getGuild().getId());

		Member mb = event.getMember();
		Role join = config.getSettings().getJoinRole();
		if (join != null && event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
			event.getGuild().addRoleToMember(mb, join).queue();
		}

		WelcomeSettings ws = config.getWelcomeSettings();
		TextChannel channel = ws.getChannel();
		if (channel != null) {
			buildAndSendJLEmbed(config, channel, mb, ws.getMessage(), ws.getHeaders(), true);
		}
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		if (event.getUser().isBot()) return;

		GuildConfig config = DAO.find(GuildConfig.class, event.getGuild().getId());

		GoodbyeSettings gs = config.getGoodbyeSettings();
		TextChannel channel = gs.getChannel();
		if (channel != null) {
			Member mb = event.getMember();
			if (mb != null) {
				buildAndSendJLEmbed(config, channel, mb, gs.getMessage(), gs.getHeaders(), false);
			}
		}
	}

	private void buildAndSendJLEmbed(GuildConfig config, TextChannel channel, Member mb, String message, Set<String> headers, boolean join) {
		GuildSettings settings = config.getSettings();

		EmbedBuilder eb = new AutoEmbedBuilder(Utils.replaceTags(mb, mb.getGuild(), settings.getEmbed().toString()))
				.setDescription(Utils.replaceTags(mb, mb.getGuild(), message))
				.setTitle(Utils.replaceTags(mb, mb.getGuild(), config.getLocale().get(Utils.getRandomEntry(headers))));

		MessageEmbed temp = eb.build();
		if (temp.getThumbnail() == null) {
			eb.setThumbnail(mb.getEffectiveAvatarUrl());
		}
		if (temp.getFooter() == null) {
			eb.setFooter("ID: " + mb.getId());
		}
		if (temp.getColor() == null) {
			eb.setColor(Graph.getColor(mb.getEffectiveAvatarUrl()));
		}

		Role welcomer = config.getSettings().getWelcomer();
		if (join && welcomer != null) {
			channel.sendMessage(welcomer.getAsMention()).setEmbeds(eb.build()).queue();
		} else {
			channel.sendMessageEmbeds(eb.build()).queue();
		}
	}

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		onGuildMessageReceived(new GuildMessageReceivedEvent(
				event.getJDA(),
				event.getResponseNumber(),
				event.getMessage()
		));
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		if (event.getAuthor().isBot() || !event.getChannel().canTalk()) return;

		String content = event.getMessage().getContentRaw();
		MessageData.Guild data;
		try {
			data = new MessageData.Guild(event);
		} catch (NullPointerException e) {
			return;
		}

		GuildConfig config = DAO.find(GuildConfig.class, data.guild().getId());
		I18N locale = config.getLocale();
		if (!Objects.equals(config.getName(), data.guild().getName())) {
			config.setName(data.guild().getName());
			config.save();
		}

		if (config.getSettings().isFeatureEnabled(GuildFeature.ANTI_ZALGO)) {
			Member mb = event.getMember();
			if (mb != null && event.getGuild().getSelfMember().hasPermission(Permission.NICKNAME_MANAGE)) {
				String name = Unidecode.decode(mb.getEffectiveName()).trim();
				if (!name.equals(mb.getEffectiveName())) {
					if (name.length() < 3) {
						name = locale.get("zalgo/name_" + Calc.rng(1, 4));
					}

					mb.modifyNickname(name).queue();
				}
			}
		}

		Profile profile = DAO.find(Profile.class, new ProfileId(data.user().getId(), data.guild().getId()));
		int lvl = profile.getLevel();

		profile.addXp(15);
		profile.save();

		Account account = profile.getAccount();
		if (!Objects.equals(account.getName(), data.user().getName())) {
			account.setName(data.user().getName());
			account.save();
		}

		AtomicReference<TextChannel> notifs = new AtomicReference<>();
		if (config.getSettings().isFeatureEnabled(GuildFeature.NOTIFICATIONS)) {
			notifs.set(config.getSettings().getNotificationsChannel());
			if (notifs.get() == null) {
				notifs.set(event.getChannel());
			}
		}

		if (profile.getLevel() > lvl) {
			int high = account.getHighestLevel();
			int prize = 0;
			if (profile.getLevel() > high) {
				prize = profile.getLevel() * 150;
				account.addCR(prize, "Level up prize");
			}

			if (notifs.get() != null) {
				notifs.get().sendMessage(locale.get(prize > 0 ? "str/level_up_prize" : "str/level_up", data.user().getAsMention(), profile.getLevel(), prize)).queue(null, Utils::doNothing);
			}
		}

		DynamicProperty dp = account.getDynamicProperty("message_count");
		int count = NumberUtils.toInt(dp.getValue()) + 1;
		dp.setValue(count);
		dp.save();

		DAO.apply(Account.class, account.getUid(), acc -> {
			Title t = acc.checkTitles();
			if (t != null && notifs.get() != null) {
				notifs.get().sendMessage(locale.get("achievement/title", event.getAuthor().getAsMention(), t.getInfo(locale).getName())).queue();
			}
		});

		if (toHandle.containsKey(data.guild().getId())) {
			List<SimpleMessageListener> evts = getHandler().get(data.guild().getId());
			for (SimpleMessageListener evt : evts) {
				if (!evt.isClosed() && evt.checkChannel(data.channel())) {
					evt.onGuildMessageReceived(event);
				}
			}
			evts.removeIf(SimpleMessageListener::isClosed);
		}

		EventData ed = new EventData(config, profile);
		if (content.toLowerCase().startsWith(config.getPrefix())) {
			processCommand(data, ed, content);
		}

		if (PatternCache.matches(data.message().getContentRaw(), "<@!?" + Main.getApp().getId() + ">")) {
			data.channel().sendMessage(locale.get("str/mentioned",
					data.user().getAsMention(),
					config.getPrefix(),
					Constants.SERVER_ROOT
			)).queue();
		}

		if (!event.getAuthor().equals(event.getJDA().getSelfUser()) && Utils.between(content.length(), 3, 256)) {
			List<CustomAnswer> cas = DAO.queryAll(CustomAnswer.class, "SELECT ca FROM CustomAnswer ca WHERE id.gid = ?1 AND LOWER(?2) LIKE LOWER(trigger)",
					data.guild().getId(), StringUtils.stripAccents(content)
			);

			for (CustomAnswer ca : cas) {
				if (ca.getChannels().isEmpty() || ca.getChannels().contains(event.getChannel().getId())) {
					if (ca.getUsers().isEmpty() || ca.getUsers().contains(event.getAuthor().getId())) {
						if (Calc.chance(ca.getChance() / (event.getAuthor().isBot() ? 2d : 1d))) {
							event.getChannel().sendTyping()
									.delay(ca.getAnswer().length() / 3, TimeUnit.SECONDS)
									.flatMap(v -> event.getChannel().sendMessage(ca.getAnswer()))
									.queue();
							break;
						}
					}
				}
			}
		}

		KawaiponCard kc = Spawn.getKawaipon(event.getChannel());
		if (kc != null) {
			EmbedBuilder eb = new EmbedBuilder()
					.setAuthor(locale.get("str/card_spawn", locale.get("rarity/" + kc.getCard().getRarity().name())))
					.setTitle(kc + " (" + kc.getCard().getAnime() + ")")
					.setColor(kc.getCard().getRarity().getColor(kc.isChrome()))
					.setImage("attachment://card.png")
					.setFooter(locale.get("str/card_instructions", config.getPrefix(), kc.getPrice()));

			event.getChannel().sendMessageEmbeds(eb.build())
					.addFile(IO.getBytes(kc.getCard().drawCard(kc.isChrome()), "png"), "card.png")
					.delay(1, TimeUnit.MINUTES)
					.flatMap(Message::delete)
					.queue(null, Utils::doNothing);
		}

		Drop<?> drop = Spawn.getDrop(event.getChannel());
		if (drop != null) {
			Random rng = drop.getRng();

			EmbedBuilder eb = new EmbedBuilder()
					.setAuthor(locale.get("str/drop_spawn", drop.getRarity().getIndex()))
					.setColor(drop.getRarity().getColor(false))
					.setDescription(drop.getContent().toString(locale))
					.setFooter(locale.get("str/drop_instructions", config.getPrefix(), drop.getCaptcha(true)))
					.addField(
							locale.get("str/drop_requirements"),
							drop.getConditions().stream()
									.map(dc -> dc.toString(locale, rng))
									.collect(Collectors.joining("\n")),
							true
					)
					.addField("Captcha", "`" + drop.getCaptcha(true) + "`", true);

			event.getChannel().sendMessageEmbeds(eb.build())
					.delay(1, TimeUnit.MINUTES)
					.flatMap(Message::delete)
					.queue(null, Utils::doNothing);
		}

		messages.computeIfAbsent(data.guild().getId(), k ->
				ExpiringMap.builder()
						.maxSize(64)
						.expiration(1, TimeUnit.DAYS)
						.build()
		).put(data.message().getId(), data.message());
	}

	private void processCommand(MessageData.Guild data, EventData event, String content) {
		I18N locale = event.config().getLocale();
		String[] args = content.toLowerCase().split("\\s+");
		String name = StringUtils.stripAccents(args[0].replaceFirst(event.config().getPrefix(), ""));

		String[] parts = name.split("\\.");
		JSONObject aliases = event.config().getSettings().getAliases();
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];

			if (aliases.has(part)) {
				parts[i] = aliases.getString(part);
			}
		}
		name = String.join(".", parts);

		PreparedCommand pc = Main.getCommandManager().getCommand(name);
		if (pc != null) {
			Permission[] missing = pc.getMissingPerms(data.channel());

			if (!Constants.MOD_PRIVILEGE.apply(data.member())) {
				if (event.config().getSettings().getDeniedChannels().stream().anyMatch(t -> t.equals(data.channel()))) {
					data.channel().sendMessage(locale.get("error/denied_channel")).queue();
					return;
				} else if (event.config().getSettings().getDisabledCategories().contains(pc.category())) {
					data.channel().sendMessage(locale.get("error/disabled_category")).queue();
					return;
				} else if (event.config().getSettings().getDisabledCommands().contains(pc.command().getClass().getCanonicalName())) {
					data.channel().sendMessage(locale.get("error/disabled_command")).queue();
					return;
				}
			}

			if (missing.length > 0) {
				XStringBuilder sb = new XStringBuilder(locale.get("error/missing_perms"));
				for (Permission perm : missing) {
					sb.appendNewLine("- " + locale.get("perm/" + perm.name()));
				}

				data.channel().sendMessage(sb.toString()).queue();
				return;
			}

			if (event.profile().getAccount().isBlacklisted()) {
				data.channel().sendMessage(locale.get("error/blacklisted")).queue();
				return;
			} else if (!pc.category().check(data.member())) {
				data.channel().sendMessage(locale.get("error/not_allowed")).queue();
				return;
			} else if (ratelimit.containsKey(data.user().getId())) {
				data.channel().sendMessage(locale.get("error/ratelimited")).queue();
				return;
			}

			try {
				JSONObject params = SignatureParser.parse(locale, pc.command(), content.substring(args[0].length()).trim());

				Executable.POOL.submit(() -> {
					try {
						pc.command().execute(data.guild().getJDA(), event.config().getLocale(), event, data, params);
					} catch (Exception e) {
						data.channel().sendMessage(locale.get("error/error", e)).queue();
						Constants.LOGGER.error(e, e);
					}
				});

				if (!Constants.STF_PRIVILEGE.apply(data.member())) {
					ratelimit.put(data.user().getId(), true, Calc.rng(2000, 3500), TimeUnit.MILLISECONDS);
				}
			} catch (InvalidSignatureException e) {
				String error;

				if (e.getOptions().length > 0) {
					error = locale.get("error/invalid_option").formatted(
							Utils.properlyJoin(locale.get("str/or")).apply(List.of(e.getOptions()))
					) + "```css\n%s%s %s```".formatted(
							event.config().getPrefix(),
							name,
							e.getMessage().replace("`", "'")
					);

					data.channel().sendMessage(error).queue();
				} else {
					error = locale.get("error/invalid_signature");

					List<String> signatures = SignatureParser.extract(locale, pc.command());
					EmbedBuilder eb = new ColorlessEmbedBuilder()
							.setAuthor(locale.get("str/command_signatures"))
							.setDescription("```css\n" + String.join("\n", signatures).formatted(
									event.config().getPrefix(),
									name
							) + "\n```");

					data.channel().sendMessage(error).setEmbeds(eb.build()).queue();
				}
			}
		}
	}

	public static List<Message> getMessages(Guild guild) {
		return List.copyOf(Utils.getOr(messages.get(guild.getId()), Map.<String, Message>of()).values());
	}

	public static Map<String, CopyOnWriteArrayList<SimpleMessageListener>> getHandler() {
		return Collections.unmodifiableMap(toHandle);
	}

	public static void addHandler(Guild guild, SimpleMessageListener sml) {
		toHandle.computeIfAbsent(guild.getId(), k -> new CopyOnWriteArrayList<>()).add(sml);
	}
}
