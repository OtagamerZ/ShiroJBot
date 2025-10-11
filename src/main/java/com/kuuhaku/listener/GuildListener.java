/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.Application;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.InvalidSyntaxException;
import com.kuuhaku.interfaces.annotations.Seasonal;
import com.kuuhaku.model.common.*;
import com.kuuhaku.model.common.drop.CandyDrop;
import com.kuuhaku.model.common.drop.Drop;
import com.kuuhaku.model.common.special.PadoruEvent;
import com.kuuhaku.model.common.special.SpecialEvent;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.*;
import com.kuuhaku.model.persistent.localized.LocalizedTitle;
import com.kuuhaku.model.persistent.shiro.CommandMetrics;
import com.kuuhaku.model.persistent.user.*;
import com.kuuhaku.model.records.*;
import com.kuuhaku.model.records.embed.Embed;
import com.kuuhaku.util.*;
import com.ygimenez.json.JSONObject;
import me.xuender.unidecode.Unidecode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.entities.channel.concrete.TextChannelImpl;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuildListener extends ListenerAdapter {
	private static final ExpiringMap<String, Boolean> ratelimit = ExpiringMap.builder().variableExpiration().build();
	private static final Map<String, List<SimpleMessageListener>> toHandle = new ConcurrentHashMap<>();
	private static final ExecutorService asyncExec = Executors.newWorkStealingPool();

	@Override
	public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
		if (!Application.READY || !event.isFromGuild()) return;

		User usr = event.getUser();
		if (usr == null || usr.isBot()) return;

		GuildMessageChannel chn = event.getGuildChannel();
		if (!event.getGuild().getSelfMember().hasPermission(chn, Permission.MESSAGE_HISTORY)) return;

		MessageReaction reaction = event.getReaction();
		if (reaction.getEmoji().getFormatted().equals("⭐")) {
			GuildConfig config = DAO.find(GuildConfig.class, event.getGuild().getId());

			GuildMessageChannel channel = config.getSettings().getStarboardChannel();
			if (channel == null) return;

			int stars = (int) reaction.retrieveUsers().stream()
					.filter(u -> !u.isBot() && !u.getId().equals(event.getMessageAuthorId()))
					.count();

			if (stars >= config.getSettings().getStarboardThreshold()) {
				Message msg = Pages.subGet(event.retrieveMessage());
				if (DAO.find(StarredMessage.class, msg.getId()) == null) {
					Member author = Pages.subGet(event.getGuild().retrieveMemberById(event.getMessageAuthorId()));
					if (author == null) return;

					new StarredMessage(msg.getId()).save();

					Message.Attachment img = null;
					for (Message.Attachment att : msg.getAttachments()) {
						if (att.isImage()) {
							img = att;
							break;
						}
					}

					Message ref = msg.getReferencedMessage();
					EmbedBuilder eb = new EmbedBuilder()
							.setColor(Color.ORANGE)
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

					if (!eb.isEmpty()) {
						channel.sendMessage(":star: | " + author.getAsMention() + " " + event.getChannel().getAsMention())
								.setEmbeds(eb.build())
								.queue();
					}
				}
			}
		}
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		if (!Application.READY || event.getUser().isBot()) return;

		GuildConfig config = DAO.find(GuildConfig.class, event.getGuild().getId());

		Member me = event.getGuild().getSelfMember();
		Member mb = event.getMember();
		Role join = config.getSettings().getJoinRole();
		if (join != null && me.canInteract(join) && me.hasPermission(Permission.MANAGE_ROLES)) {
			event.getGuild().addRoleToMember(mb, join).queue();
		}

		WelcomeSettings ws = config.getWelcomeSettings();
		GuildMessageChannel channel = ws.getChannel();
		if (channel != null && channel.canTalk()) {
			buildAndSendJLEmbed(config, channel, mb, ws.getMessage(), ws.getHeaders(), true);
		}
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		if (!Application.READY || event.getUser().isBot()) return;

		GuildConfig config = DAO.find(GuildConfig.class, event.getGuild().getId());

		GoodbyeSettings gs = config.getGoodbyeSettings();
		GuildMessageChannel channel = gs.getChannel();
		if (channel != null && channel.canTalk()) {
			Member mb = event.getMember();
			if (mb != null) {
				buildAndSendJLEmbed(config, channel, mb, gs.getMessage(), gs.getHeaders(), false);
			}
		}
	}

	private void buildAndSendJLEmbed(GuildConfig config, GuildMessageChannel channel, Member mb, String message, Set<String> headers, boolean join) {
		GuildSettings settings = config.getSettings();

		I18N locale = config.getLocale();
		AutoEmbedBuilder auto = new AutoEmbedBuilder(Utils.replaceTags(locale, mb, mb.getGuild(), settings.getEmbed().toString()));

		EmbedBuilder eb = auto
				.setDescription(Utils.replaceTags(locale, mb, mb.getGuild(), message))
				.setTitle(Utils.replaceTags(locale, mb, mb.getGuild(), config.getLocale().get(Utils.getRandomEntry(headers))));

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

		MessageCreateAction act = channel.sendMessage("");

		Embed model = auto.getEmbed();
		Role welcomer = config.getSettings().getWelcomer();
		if (join && welcomer != null) {
			act = act.setContent(welcomer.getAsMention());
		}

		if (model.image() != null) {
			if (join) eb.setImage(model.image().getRandomJoin());
			else eb.setImage(model.image().getRandomLeave());
		}

		act.setEmbeds(eb.build()).queue();
	}

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		if (!Application.READY) return;

		onMessageReceived(new MessageReceivedEvent(
				event.getJDA(),
				event.getResponseNumber(),
				event.getMessage()
		));
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (!Application.READY || !event.isFromGuild() || event.getAuthor().isBot() || !event.getChannel().canTalk())
			return;

		String content = event.getMessage().getContentRaw();
		MessageData.Guild data;
		try {
			data = new MessageData.Guild(event.getMessage());
			if (data.member() == null) return;
		} catch (NullPointerException e) {
			return;
		}

		if (toHandle.containsKey(data.guild().getId())) {
			List<SimpleMessageListener> evts = getHandler().get(data.guild().getId());
			for (SimpleMessageListener evt : evts) {
				if (!evt.isClosed() && evt.checkChannel(data.channel())) {
					evt.execute(event);
				}
			}
			evts.removeIf(SimpleMessageListener::isClosed);
		}

		GuildConfig config = DAO.find(GuildConfig.class, data.guild().getId());
		I18N locale = config.getLocale(event.getGuildChannel());
		if (!Objects.equals(config.getName(), data.guild().getName())) {
			config.setName(data.guild().getName());
			config.save();
		}

		if (config.getSettings().isFeatureEnabled(GuildFeature.ANTI_LINK)) {
			Matcher m = Utils.regex(content, "(ht|f)tps?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*");
			if (m.find() && data.me().hasPermission(data.channel(), Permission.MESSAGE_MANAGE)) {
				data.message().delete().queue(null, Utils::doNothing);
				return;
			}
		}

		Account account = DAO.find(Account.class, data.user().getId());
		if (!Objects.equals(account.getName(), data.user().getName())) {
			account.setName(data.user().getName());
			account.save();
		}

		Profile profile = account.getProfile(data.member());
		int lvl = profile.getLevel();

		GuildBuff gb = config.getCumBuffs();
		profile.addXp((int) (15 * (1 + gb.xp()) * (1 + (account.getStreak() / 100d))));

		asyncExec.execute(() -> {
			Thread.currentThread().setName("Event-" + Thread.currentThread().threadId());
			EventData ed = new EventData(data.channel(), config, profile);
			if (content.toLowerCase().startsWith(config.getPrefix()) && data.channel().canTalk()) {
				processCommand(data, ed, content);
			}

			if (config.getSettings().isFeatureEnabled(GuildFeature.ANTI_ZALGO)) {
				Member mb = data.member();
				if (data.me().hasPermission(Permission.NICKNAME_MANAGE) && data.me().canInteract(mb)) {
					String name = Unidecode.decode(mb.getEffectiveName()).trim();
					if (!name.equals(mb.getEffectiveName())) {
						if (name.length() < 3) {
							name = locale.get("zalgo/name_" + Calc.rng(1, 4));
						}

						mb.modifyNickname(name).queue();
					}
				}
			}

			if (profile.getLevel() > lvl) {
				int high = account.getHighestLevel();
				int prize = 0;

				profile.applyXp();
				if (profile.getLevel() > high) {
					prize = profile.getLevel() * 150;
					account.addCR(prize, "Level up prize");

					ed.notify(locale.get("achievement/level_up_prize", data.user().getAsMention(), profile.getLevel(), prize));

					if (profile.getLevel() <= 19) {
						UserItem item = DAO.find(UserItem.class, "STARTER_TOKEN");

						account.addItem(item, 2);
						ed.notify(locale.get("str/received_item", 2, item.getName(locale)));
					}
				} else {
					ed.notify(locale.get("achievement/level_up", data.user().getAsMention(), profile.getLevel(), prize));
				}

				if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER) {
					UserItem item = DAO.find(UserItem.class, "SPOOKY_CANDY");

					account.addItem(item, profile.getLevel() * 2);
					ed.notify(locale.get("str/received_item", profile.getLevel() * 2, item.getName(locale)));
				}
			}

			if (data.guild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
				Map<Integer, List<LevelRole>> roles = config.getSettings().getLevelRoles().parallelStream()
						.filter(lr -> lr.getLevel() <= profile.getLevel())
						.collect(Collectors.groupingBy(LevelRole::getLevel));

				if (!roles.isEmpty()) {
					List<Role> toAdd = roles.entrySet().parallelStream()
							.max(Map.Entry.comparingByKey())
							.map(Map.Entry::getValue)
							.orElse(List.of())
							.parallelStream()
							.map(LevelRole::getRole)
							.filter(Objects::nonNull)
							.filter(r -> data.me().canInteract(r) && !r.isManaged())
							.toList();

					List<Role> toRemove = roles.values().parallelStream()
							.flatMap(List::parallelStream)
							.map(LevelRole::getRole)
							.filter(Objects::nonNull)
							.filter(r -> !toAdd.contains(r))
							.filter(r -> data.me().canInteract(r) && !r.isManaged())
							.toList();

					List<Role> toAnnounce = toAdd.parallelStream()
							.filter(r -> !data.member().getRoles().contains(r))
							.toList();

					if (!toAnnounce.isEmpty() && profile.getLevel() > lvl) {
						ed.notify(locale.get("str/level_role_earn",
								Utils.properlyJoin(locale.get("str/and")).apply(toAnnounce.stream().map(Role::getAsMention).toList()))
						);
					}

					data.guild().modifyMemberRoles(data.member(), toAdd, toRemove).queue(null, Utils::doNothing);
				}
			}

			if (Utils.match(data.message().getContentRaw(), "<@!?" + Main.getApp().getId() + ">")) {
				if (!data.channel().canTalk()) {
					data.user().openPrivateChannel()
							.flatMap(c -> c.sendMessage(locale.get("str/cant_talk", data.channel().getAsMention(), locale.get("perm/message_send"))))
							.queue(null, Utils::doNothing);
				} else {
					data.channel().sendMessage(locale.get("str/mentioned",
							data.user().getAsMention(),
							config.getPrefix(),
							Constants.SERVER_ROOT)
					).queue(null, Utils::doNothing);
				}
			}

			if (config.getSettings().isFeatureEnabled(GuildFeature.NQN_MODE)) {
				Member mb = data.member();
				boolean proxy = false;

				StringBuilder sb = new StringBuilder();
				for (String s : content.split(" ")) {
					sb.append(" ");

					if (!s.isBlank()) {
						String name = Utils.extract(s, "^:([\\w-]+):$", 1);
						if (name != null) {
							RichCustomEmoji emj = null;

							List<RichCustomEmoji> valid = Main.getApp().getShiro().getEmojisByName(name, true);
							if (!valid.isEmpty()) {
								for (RichCustomEmoji e : valid) {
									if (e.getGuild().equals(data.guild())) {
										emj = e;
										break;
									}
								}

								if (emj == null) {
									emj = valid.parallelStream()
											.filter(e -> e.getGuild().isMember(mb))
											.findAny()
											.orElse(valid.getFirst());
								}
							}

							if (emj != null) {
								sb.append(emj.getAsMention());
								proxy = true;
								continue;
							}
						}
					}

					sb.append(s);
				}

				if (proxy) {
					PseudoUser pu = new PseudoUser(mb, data.channel());
					pu.send(data.message(), sb.toString());
				}
			}

			if (!data.member().equals(data.me()) && Utils.between(content.length(), 3, 255)) {
				List<CustomAnswer> cas = DAO.queryAll(CustomAnswer.class, "SELECT ca FROM CustomAnswer ca WHERE id.gid = ?1 AND LOWER(?2) LIKE LOWER(trigger)",
						data.guild().getId(), StringUtils.stripAccents(content)
				);

				for (CustomAnswer ca : cas) {
					if (ca.getChannels().isEmpty() || ca.getChannels().contains(data.channel().getId())) {
						if (ca.getUsers().isEmpty() || ca.getUsers().contains(data.user().getId())) {
							if (Calc.chance(ca.getChance() / (data.user().isBot() ? 2d : 1d))) {
								data.channel().sendTyping()
										.delay(ca.getAnswer().length() / 3, TimeUnit.SECONDS)
										.flatMap(v -> data.channel().sendMessage(ca.getAnswer()))
										.queue();
								break;
							}
						}
					}
				}
			}

			rollSpawns(config, locale, data.user());
			if (!config.getSettings().isFeatureEnabled(GuildFeature.NO_SEASON)) {
				rollEvents(data.channel(), locale);
			}
		});
	}

	private void rollSpawns(GuildConfig config, I18N locale, User u) {
		GuildBuff gb = config.getCumBuffs();
		Set<TextChannelImpl> channels = config.getSettings().getKawaiponChannels();
		if (!channels.isEmpty() && Calc.chance(100d / channels.size())) {
			GuildMessageChannel chosen = Utils.getRandomEntry(channels);
			if (!chosen.canTalk()) return;

			StashedCard sc = Spawn.getKawaipon(locale, gb, chosen, u);
			if (sc != null) {
				EmbedBuilder eb = new EmbedBuilder()
						.setAuthor(locale.get("str/card_spawn", locale.get("rarity/" + sc.getCard().getRarity())))
						.setTitle(sc + " (" + sc.getCard().getAnime() + ")")
						.setColor(sc.getCard().getRarity().getColor(sc.isChrome()))
						.setImage("attachment://card.png")
						.setFooter(locale.get("str/card_instructions", config.getPrefix(), sc.getCollectPrice()));

				chosen.sendMessageEmbeds(eb.build())
						.addFiles(FileUpload.fromData(IO.getBytes(sc.render(), "png"), "card.png"))
						.delay((long) (60 / Spawn.getQuantityMult()), TimeUnit.SECONDS)
						.flatMap(Message::delete)
						.queue(null, Utils::doNothing);
			}
		}

		channels = config.getSettings().getDropChannels();
		if (!channels.isEmpty() && Calc.chance(100d / channels.size())) {
			GuildMessageChannel chosen = Utils.getRandomEntry(channels);
			if (!chosen.canTalk()) return;

			Drop drop = Spawn.getDrop(locale, gb, chosen, u);
			if (drop != null) {
				RandomGenerator rng = drop.getRng();

				EmbedBuilder eb = new EmbedBuilder()
						.setAuthor(locale.get("str/drop_spawn", drop.getRarity().getIndex()))
						.setColor(drop.getRarity().getColor(false))
						.setDescription(drop.getContent(locale))
						.setFooter(locale.get("str/drop_instructions", config.getPrefix(), drop.getCaptcha(true)))
						.addField(
								locale.get("str/drop_requirements"),
								drop.getConditions().stream()
										.map(dc -> dc.toString(locale, rng))
										.collect(Collectors.joining("\n")),
								true
						)
						.addField("Captcha", "`" + drop.getCaptcha(true) + "`", true);

				if (drop instanceof CandyDrop) {
					eb.setThumbnail("https://cdn.pixabay.com/photo/2012/04/01/16/39/halloween-23439_1280.png");
				}

				chosen.sendMessageEmbeds(eb.build())
						.delay((long) (60 / Spawn.getQuantityMult()), TimeUnit.SECONDS)
						.flatMap(Message::delete)
						.queue(null, Utils::doNothing);
			}
		}
	}

	private void rollEvents(GuildMessageChannel channel, I18N locale) {
		if (SpecialEvent.hasEvent(channel.getGuild()) || !channel.canTalk()) return;

		if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER && Calc.chance(1)) {
			new PadoruEvent(locale).start(channel);
		}
	}

	private void processCommand(MessageData.Guild data, EventData event, String content) {
		I18N locale = event.config().getLocale();
		String[] args = content.toLowerCase().split("\\s+");
		String name = StringUtils.stripAccents(args[0].replaceFirst(Pattern.quote(event.config().getPrefix()), ""));

		String[] parts = name.split("\\.");
		JSONObject aliases = new JSONObject();
		aliases.putAll(event.config().getSettings().getAliases());
		aliases.putAll(event.profile().getAccount().getSettings().getAliases());

		String command = "";
		for (String s : parts) {
			if (command.isBlank()) command = s;
			else command += "." + s;

			if (aliases.has(command)) {
				command = aliases.getString(command);
			}
		}

		name = command;
		PreparedCommand pc = Main.getCommandManager().getCommand(name);
		if (pc != null) {
			if (pc.command() == null) {
				String msg = locale.get("error/surrogate_command");

				Set<PreparedCommand> subCmds = pc.getSubCommands();
				if (!subCmds.isEmpty()) {
					StringTree tree = new StringTree();

					for (PreparedCommand sub : subCmds) {
						String[] path = sub.name().split("(?=\\.)");
						path[0] = event.config().getPrefix() + path[0];

						tree.addElement((Object[]) path);
					}

					msg += " ```" + tree + "```";
				}

				data.channel().sendMessage(msg).queue();
				return;
			}

			int month = Calendar.getInstance().get(Calendar.MONTH);
			Seasonal season = pc.command().getClass().getDeclaredAnnotation(Seasonal.class);
			if (season != null) {
				String error = null;
				if (season.months().length > 0 && !ArrayUtils.contains(season.months(), month)) {
					error = "out_of_season";
				} else if (season.exclude().length > 0 && ArrayUtils.contains(season.exclude(), month)) {
					error = "in_season";
				}

				if (error != null) {
					List<String> months = Arrays.stream(error.equals("in_season") ? season.exclude() : season.months())
							.mapToObj(m -> Month.of(m + 1))
							.map(m -> locale.get("month/" + m))
							.toList();

					data.channel().sendMessage(locale.get("error/" + error,
							Utils.properlyJoin(locale.get("str/or")).apply(months)
					)).queue();
					return;
				}
			}

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
					sb.appendNewLine("- " + locale.get("perm/" + perm));
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
			} else if (pc.isBeta() && !event.config().getGid().equals(Constants.SUPPORT_SERVER)) {
				data.channel().sendMessage(locale.get("error/beta_command")).queue();
				return;
			} else if (ratelimit.containsKey(data.user().getId())) {
				data.channel().sendMessage(locale.get("error/ratelimited")).queue();
				return;
			}

			try {
				JSONObject params = SyntaxParser.parse(locale, pc.command(), content.substring(args[0].length()).trim());

				Exception error = null;
				Instant start = Instant.now();
				try {
					pc.command().execute(data.guild().getJDA(), event.config().getLocale(), event, data, params);
				} catch (Exception e) {
					Constants.LOGGER.error(e, e);
					data.channel().sendMessage(locale.get("error/error", e)).queue();
					error = e;
				}

				if (!Constants.STF_PRIVILEGE.apply(data.member())) {
					int runtime = Math.toIntExact(Duration.between(start, Instant.now()).toMillis());
					new CommandMetrics(
							data.user().getId(),
							data.guild().getId(),
							pc.name(),
							runtime, error
					).save();

					ratelimit.put(data.user().getId(), true, Calc.rng(2000, 3500), TimeUnit.MILLISECONDS);
				}
			} catch (InvalidSyntaxException e) {
				String error;

				error = locale.get("error/invalid_syntax");

				List<String> syntaxes = SyntaxParser.extract(locale, pc.command());
				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setAuthor(locale.get("str/command_syntaxes"))
						.setDescription("```css\n" + String.join("\n", syntaxes).formatted(
								event.config().getPrefix(),
								name
						) + "\n```");

				data.channel().sendMessage(error).setEmbeds(eb.build()).queue();
			} finally {
				DAO.apply(Account.class, data.user().getId(), acc -> {
					Title t = acc.checkTitles(locale);
					if (t != null) {
						LocalizedTitle info = t.getInfo(locale);
						event.notify(
								locale.get("achievement/title", data.user().getAsMention(), info.getName())
								+ "\n> " + info.getDescription()
						);
					}
				});
			}
		}
	}

	public static Map<String, List<SimpleMessageListener>> getHandler() {
		return Collections.unmodifiableMap(toHandle);
	}

	public static void addHandler(Guild guild, SimpleMessageListener sml) {
		toHandle.computeIfAbsent(guild.getId(), k -> new CopyOnWriteArrayList<>()).add(sml);
	}
}
